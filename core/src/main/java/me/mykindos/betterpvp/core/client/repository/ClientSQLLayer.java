package me.mykindos.betterpvp.core.client.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.offlinemessages.OfflineMessagesRepository;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentRepository;
import me.mykindos.betterpvp.core.client.rewards.RewardBox;
import me.mykindos.betterpvp.core.client.stats.StatBuilder;
import me.mykindos.betterpvp.core.client.stats.StatConcurrentHashMap;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.jooq.tables.records.ClientsRecord;
import me.mykindos.betterpvp.core.database.mappers.PropertyMapper;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.DoubleStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.SnowflakeIdGenerator;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static me.mykindos.betterpvp.core.database.jooq.Tables.*;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@CustomLog
@Singleton
public class ClientSQLLayer {

    private final Database database;
    private final PropertyMapper propertyMapper;
    private final StatBuilder statBuilder;
    @Getter
    private final PunishmentRepository punishmentRepository;

    private final OfflineMessagesRepository offlineMessagesRepository;

    private final AtomicReference<ConcurrentHashMap<String, ConcurrentHashMap<String, Query>>> queuedPropertyUpdates;
    private final AtomicReference<ConcurrentHashMap<String, ConcurrentHashMap<String, Query>>> queuedSharedPropertyUpdates;
    private final AtomicReference<ConcurrentHashMap<String, ConcurrentHashMap<String, Query>>> queuedStatUpdates;
    private static final ThreadLocal<Map<UUID, Client>> LOADING_CLIENTS = ThreadLocal.withInitial(HashMap::new);
    private static final SnowflakeIdGenerator ID_GENERATOR = new SnowflakeIdGenerator();

    @Inject
    public ClientSQLLayer(Database database, PropertyMapper propertyMapper, StatBuilder statBuilder, PunishmentRepository punishmentRepository, OfflineMessagesRepository offlineMessagesRepository) {
        this.database = database;
        this.propertyMapper = propertyMapper;
        this.statBuilder = statBuilder;
        this.punishmentRepository = punishmentRepository;
        this.offlineMessagesRepository = offlineMessagesRepository;
        this.queuedPropertyUpdates = new AtomicReference<>(new ConcurrentHashMap<>());
        this.queuedSharedPropertyUpdates = new AtomicReference<>(new ConcurrentHashMap<>());
        this.queuedStatUpdates = new AtomicReference<>(new ConcurrentHashMap<>());
    }

    public Client create(UUID uuid, String name) {
        long id = ID_GENERATOR.nextId();
        final Gamer gamer = new Gamer(id, uuid.toString());
        final Client created = new Client(id, gamer, uuid.toString(), name, Rank.PLAYER);
        save(created);
        created.setNewClient(true);
        return created;
    }

    public Optional<Client> getAndUpdate(UUID uuid) {
        return getClient(uuid);
    }

    public Optional<Client> getClient(@Nullable UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }

        // Check if this client is already being loaded by UUID
        Map<UUID, Client> loadingClients = LOADING_CLIENTS.get();
        Client loadingClient = loadingClients.get(uuid);

        if (loadingClient != null) {
            // We're already loading this client - return the partially loaded client
            log.warn("Returning partially loaded client for uuid: {}", uuid).submit();
            return Optional.of(loadingClient);
        }

        try {
            DSLContext ctx = database.getDslContext();

            ClientsRecord clientsRecord = ctx.selectFrom(CLIENTS)
                    .where(CLIENTS.UUID.eq(uuid.toString()))
                    .fetchOne();
            if (clientsRecord != null) {
                long id = clientsRecord.getId();
                String name = clientsRecord.getName();

                Rank rank = Rank.PLAYER;
                try {
                    rank = Rank.valueOf(clientsRecord.getRank());
                } catch (IllegalArgumentException ex) {
                    log.warn("Invalid rank for " + name + " (" + uuid + ")").submit();
                }

                Gamer gamer = new Gamer(id, uuid.toString());
                Client client = new Client(id, gamer, uuid.toString(), name, rank);
                loadingClients.put(uuid, client);
                loadAdditionalClientData(client);

                return Optional.of(client);

            } else {
                log.warn("Failed to find client with UUID {}", uuid.toString()).submit();
            }
        } catch (DataAccessException ex) {
            log.error("Error loading client: {}", uuid.toString(), ex).submit();
        } finally {
            loadingClients.remove(uuid);
        }

        return Optional.empty();

    }

    public Optional<Client> getClient(@Nullable String name) {
        if (name == null) {
            return Optional.empty();
        }

        try {
            ClientsRecord clientRecord = database.getDslContext().selectFrom(CLIENTS)
                    .where(CLIENTS.NAME.eq(name))
                    .fetchOne();

            if (clientRecord != null) {
                UUID uuid = UUID.fromString(clientRecord.get(CLIENTS.UUID));
                String actualName = clientRecord.get(CLIENTS.NAME);
                Rank rank = Rank.valueOf(clientRecord.get(CLIENTS.RANK));
                long clientId = clientRecord.get(CLIENTS.ID);

                Gamer gamer = new Gamer(clientId, uuid.toString());
                Client client = new Client(clientId, gamer, uuid.toString(), actualName, rank);

                loadAdditionalClientData(client);
                return Optional.of(client);
            }
        } catch (Exception ex) {
            log.error("Error loading client", ex).submit();
        }

        return Optional.empty();
    }

    /**
     * Loads additional client data concurrently (punishments, ignores, properties).
     *
     * @param client The client to load data for
     */
    private void loadAdditionalClientData(Client client) {
        CompletableFuture<List<Punishment>> punishmentsFuture = CompletableFuture.supplyAsync(() ->
                punishmentRepository.getPunishmentsForClient(client));
        CompletableFuture<Set<UUID>> ignoresFuture = CompletableFuture.supplyAsync(() ->
                getIgnoresForClient(client));
        CompletableFuture<Void> propertiesFuture = loadAllPropertiesConcurrently(client);

        CompletableFuture.allOf(punishmentsFuture, ignoresFuture, propertiesFuture).join();
        client.getPunishments().addAll(punishmentsFuture.join());
        client.getIgnores().addAll(ignoresFuture.join());
    }


    private Set<UUID> getIgnoresForClient(Client client) {
        Set<UUID> ignores = new HashSet<>();

        try {
            database.getDslContext().select(CLIENTS.UUID)
                    .from(IGNORES)
                    .innerJoin(CLIENTS).on(IGNORES.IGNORED.eq(CLIENTS.ID))
                    .where(IGNORES.CLIENT.eq(client.getId()))
                    .fetch()
                    .forEach(ignoreRecord -> {
                        UUID ignored = UUID.fromString(ignoreRecord.get(CLIENTS.UUID));
                        ignores.add(ignored);
                    });
        } catch (Exception ex) {
            log.error("Error loading ignores for client {}", client.getUuid(), ex).submit();
        }

        return ignores;
    }

    public int getTotalClients() {
        return database.getDslContext().fetchCount(CLIENTS);
    }

    /**
     * Asynchronously loads properties from a database result and maps them to the provided property container.
     *
     * @param propertyContainer The container where the parsed properties will be stored.
     * @return A CompletableFuture that completes when the properties have been successfully loaded and mapped.
     */
    private void loadPropertiesAsync(Result<Record2<String, String>> result,
                                     PropertyContainer propertyContainer) {
        if (result != null) {
            propertyMapper.parseProperties(result, propertyContainer);
        }
    }

    /**
     * Loads client properties asynchronously.
     *
     * @param client The client to load properties for
     * @return A CompletableFuture that completes when properties are loaded
     */
    public CompletableFuture<Void> loadClientPropertiesAsync(Client client) {
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            Result<Record2<String, String>> result = ctx.select(CLIENT_PROPERTIES.PROPERTY, CLIENT_PROPERTIES.VALUE)
                    .from(CLIENT_PROPERTIES)
                    .where(CLIENT_PROPERTIES.CLIENT.eq(client.getId()))
                    .fetch();
            loadPropertiesAsync(result, client);
        });
    }

    /**
     * Loads gamer properties asynchronously.
     *
     * @param client The client containing the gamer to load properties for
     * @return A CompletableFuture that completes when properties are loaded
     */
    public CompletableFuture<Void> loadGamerPropertiesAsync(Client client) {
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            Gamer gamer = client.getGamer();
            Result<Record2<String, String>> result = ctx.select(GAMER_PROPERTIES.PROPERTY, GAMER_PROPERTIES.VALUE)
                    .from(GAMER_PROPERTIES)
                    .where(GAMER_PROPERTIES.CLIENT.eq(client.getId()))
                    .and(GAMER_PROPERTIES.REALM.eq(Core.getCurrentRealm()))
                    .fetch();
            loadPropertiesAsync(result, gamer);
        });
    }

    public CompletableFuture<Void> loadStatsAsync(Client client) {
        final StatContainer statContainer = client.getStatContainer();

        final Statement statement = Statement.builder()
                .select("client_stats", "Period", "Statname", "Stat")
                .where("Client", "=", new UuidStatementValue(statContainer.getUniqueId()))
                .build();

        return database.executeQuery(statement, TargetDatabase.GLOBAL).thenAccept(results -> {
            final StatConcurrentHashMap tempMap = new StatConcurrentHashMap();
            try {
                while (results.next()) {
                    final String period = results.getString("Period");
                    final String statName = results.getString("Statname");
                    final IStat stat = statBuilder.getStatForStatName(statName);
                    final double value = results.getDouble("Stat");
                    try {
                        tempMap.put(period, stat, value, true);
                    } catch (Exception e) {
                        log.error("Error saving stat {} ({}), period {}, value {}", stat, statName, period, value, e).submit();
                    }

                }

            } catch (SQLException e) {
                log.info("Error loading stats for {} ", client.getName(), e).submit();
            }
            statContainer.getStats().copyFrom(tempMap);
        });


    }

    /**
     * Loads both client and gamer properties concurrently.
     *
     * @param client The client to load properties for
     * @return A CompletableFuture that completes when all properties are loaded
     */
    public CompletableFuture<Void> loadAllPropertiesConcurrently(Client client) {
        CompletableFuture<Void> clientPropertiesFuture = loadClientPropertiesAsync(client);
        CompletableFuture<Void> gamerPropertiesFuture = loadGamerPropertiesAsync(client);
        CompletableFuture<Void> statPropertiesFuture = loadStatsAsync(client);
        return CompletableFuture.allOf(clientPropertiesFuture, gamerPropertiesFuture, statPropertiesFuture);
    }


    public void save(Client object) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.transaction(config -> {
                DSLContext ctxl = DSL.using(config);

                ctxl.insertInto(CLIENTS)
                        .set(CLIENTS.ID, object.getId())
                        .set(CLIENTS.UUID, object.getUuid())
                        .set(CLIENTS.NAME, object.getName())
                        .onConflict(CLIENTS.UUID)
                        .doUpdate()
                        .set(CLIENTS.NAME, object.getName())
                        .set(CLIENTS.RANK, object.getRank().name())
                        .execute();

                // Prevent possibility of clients with same name
                // This happens if someone name changes and never joins server again, and then someone else takes their name
                ctxl.update(CLIENTS)
                        .set(CLIENTS.NAME, object.getName() + "-" + System.currentTimeMillis())
                        .where(CLIENTS.UUID.ne(object.getUuid()))
                        .and(CLIENTS.NAME.eq(object.getName()))
                        .execute();
            });
        }).exceptionally(throwable -> {
            log.error("Failed to save client " + object.getUuid(), throwable).submit();
            return null;
        });

        // Gamer
        final Gamer gamer = object.getGamer();
        gamer.getProperties().getMap().forEach((key, value) -> saveGamerProperty(gamer, key, value));
    }

    public void saveIgnore(Client client, Client ignored) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.insertInto(IGNORES)
                .set(IGNORES.CLIENT, client.getId())
                .set(IGNORES.IGNORED, ignored.getId())
                .execute());
    }

    public void removeIgnore(Client client, Client ignored) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.deleteFrom(IGNORES)
                .where(IGNORES.CLIENT.eq(client.getId()))
                .and(IGNORES.IGNORED.eq(ignored.getId()))
                .execute());
    }

    public void saveProperty(Client client, String property, Object value) {
        Query query = database.getDslContext().insertInto(CLIENT_PROPERTIES)
                .set(CLIENT_PROPERTIES.CLIENT, client.getId())
                .set(CLIENT_PROPERTIES.PROPERTY, property)
                .set(CLIENT_PROPERTIES.VALUE, value.toString())
                .onConflict(CLIENT_PROPERTIES.CLIENT, CLIENT_PROPERTIES.PROPERTY)
                .doUpdate()
                .set(CLIENT_PROPERTIES.VALUE, value.toString());


        queuedSharedPropertyUpdates.updateAndGet(map -> {
            ConcurrentHashMap<String, Query> propertyUpdates = map.computeIfAbsent(client.getUuid(), k -> new ConcurrentHashMap<>());
            propertyUpdates.put(property, query);
            return map;
        });
    }

    public void saveGamerProperty(Gamer gamer, String property, Object value) {
        Query query = database.getDslContext().insertInto(GAMER_PROPERTIES)
                .set(GAMER_PROPERTIES.CLIENT, gamer.getId())
                .set(GAMER_PROPERTIES.REALM, Core.getCurrentRealm())
                .set(GAMER_PROPERTIES.PROPERTY, property)
                .set(GAMER_PROPERTIES.VALUE, value.toString())
                .onConflict(GAMER_PROPERTIES.CLIENT, GAMER_PROPERTIES.REALM, GAMER_PROPERTIES.PROPERTY)
                .doUpdate()
                .set(GAMER_PROPERTIES.VALUE, value.toString());


        queuedPropertyUpdates.updateAndGet(map -> {
            ConcurrentHashMap<String, Query> propertyUpdates = map.computeIfAbsent(gamer.getUuid(), k -> new ConcurrentHashMap<>());
            propertyUpdates.put(property, query);
            return map;
        });
    }

    public void processStatUpdates(UUID uuid, boolean async) {
        // Process shared stat updates for this UUID
        ConcurrentHashMap<String, Query> sharedQueries = queuedSharedStatUpdates.getAndUpdate(map -> {
            final ConcurrentHashMap<String, ConcurrentHashMap<String, Query>> updated = new ConcurrentHashMap<>(map);
            updated.remove(uuid.toString());
            return updated;
        }).get(uuid.toString());

        if (sharedQueries != null && !sharedQueries.isEmpty()) {
            List<Query> queries = new ArrayList<>(sharedQueries.values());
            executeQueriesAsTransaction(queries, async);
        }

        // Process stat updates for this UUID
        ConcurrentHashMap<String, Query> gamerQueries = queuedStatUpdates.getAndUpdate(map -> {
            final ConcurrentHashMap<String, ConcurrentHashMap<String, Query>> updated = new ConcurrentHashMap<>(map);
            updated.remove(uuid.toString());
            return updated;
        }).get(uuid.toString());

        if (gamerQueries != null && !gamerQueries.isEmpty()) {
            List<Query> queries = new ArrayList<>(gamerQueries.values());
            executeQueriesAsTransaction(queries, async);
        }
    }

    public void saveStatProperty(StatContainer statContainer, String period, String statName, Double stat) {
        //TODO change to JOOQ
        String saveStatUpdate = "INSERT INTO client_stats (Client, Period, Statname, Stat) VALUES (?, ?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE Stat = ?";

        return new Statement(saveStatUpdate,
                new UuidStatementValue(statContainer.getUniqueId()),
                new StringStatementValue(period),
                new StringStatementValue(stat.getStatName()),
                new DoubleStatementValue(value),
                new DoubleStatementValue(value)
        );

        queuedStatUpdates.updateAndGet(map -> {
            ConcurrentHashMap<String, Query> statUpdatse = map.computeIfAbsent(gamer.getUuid(), k -> new ConcurrentHashMap<>());
            statUpdatse.put(property, query);
            return map;
        });
    }

    public void processStatUpdates(UUID uuid, boolean async) {
        ConcurrentHashMap<String, Query> sharedQueries = queuedSharedPropertyUpdates.updateAndGet(map -> {
            map.remove(uuid.toString());
            return map;
        }).get(uuid.toString());

        if (sharedQueries != null && !sharedQueries.isEmpty()) {
            List<Query> queries = new ArrayList<>(sharedQueries.values());
            executeQueriesAsTransaction(queries, async);
        }

        // Process stat updates for this UUID
        ConcurrentHashMap<String, Query> gamerQueries = queuedPropertyUpdates.updateAndGet(map -> {
            map.remove(uuid.toString());
            return map;
        }).get(uuid.toString());

        if (gamerQueries != null && !gamerQueries.isEmpty()) {
            List<Query> queries = new ArrayList<>(gamerQueries.values());
            executeQueriesAsTransaction(queries, async);
        }

        ConcurrentHashMap<String, Query> statQueries = queuedStatUpdates.updateAndGet(map -> {
            map.remove(uuid.toString());
            return map;
        }).get(uuid.toString());

        if (statQueries != null && statQueries.isEmpty()) {
            List<Query> queries = new ArrayList<>(statQueries.values());
            executeQueriesAsTransaction(queries, async);
        }

        log.info("Updated stats for {}", uuid).submit();
    }

    public void processStatUpdates(boolean async) {

        log.info("Beginning to process stat updates").submit();

        // Gamer - atomically swap with empty map
        ConcurrentHashMap<String, ConcurrentHashMap<String, Query>> gamerStatements =
                queuedPropertyUpdates.getAndSet(new ConcurrentHashMap<>());

        List<Query> statementsToRun = new ArrayList<>();
        gamerStatements.forEach((key, value) -> statementsToRun.addAll(value.values()));

        executeQueriesAsTransaction(statementsToRun, async);
        log.info("Updated gamer properties with {} queries", statementsToRun.size()).submit();

        // Client - atomically swap with empty map
        ConcurrentHashMap<String, ConcurrentHashMap<String, Query>> sharedStatements =
                queuedSharedPropertyUpdates.getAndSet(new ConcurrentHashMap<>());

        List<Query> sharedStatementsToRun = new ArrayList<>();
        sharedStatements.forEach((key, value) -> sharedStatementsToRun.addAll(value.values()));

        executeQueriesAsTransaction(sharedStatementsToRun, async);
        log.info("Updated client properties with {} queries", sharedStatementsToRun.size()).submit();

        oncurrentHashMap<String, ConcurrentHashMap<String, Query>> statStatements =
                queuedStatUpdates.getAndSet(new ConcurrentHashMap<>());

        List<Query> statStatementsToRun = new ArrayList<>();
        statStatements.forEach((key, value) -> statStatementsToRun.addAll(value.values()));

        executeQueriesAsTransaction(sharedStatementsToRun, async);
        log.info("Updated client stats with {} queries", sharedStatementsToRun.size()).submit();

    }

    public CompletableFuture<Void> processStatUpdates(Set<Client> clients, String period) {
        List<Statement> statementsToRun = clients.stream().flatMap(client -> getStatUpdates(client, period).stream()).toList();
        return database.executeBatch(statementsToRun, TargetDatabase.GLOBAL);
    }

    private List<Statement> getStatUpdates(Client client, String period) {
        synchronized (client.getStatContainer()) {
            log.info(client.getStatContainer().getChangedStats().toString()).submit();
            List<Statement> statementStream = client.getStatContainer().getChangedStats().stream().map(statName -> {
                        return getSaveStatProperty(client.getStatContainer(), period, statName, client.getStatContainer().getProperty(period, statName));
            }).toList();
            client.getStatContainer().getChangedStats().clear();
            return statementStream;
    /**
     * Executes a list of jOOQ queries as a single transaction.
     *
     * @param queries The list of jOOQ Query objects to execute
     */
    private void executeQueriesAsTransaction(List<Query> queries, boolean async) {
        if (queries == null || queries.isEmpty()) {
            return;
        }

        try {
            if (async) {
                database.getAsyncDslContext().executeAsyncVoid(ctx -> {
                    ctx.transaction(config -> {
                        DSLContext ctxl = DSL.using(config);
                        ctxl.batch(queries).execute();
                    });
                });
            } else {
                database.getDslContext().transaction(config -> {
                    DSLContext ctxl = DSL.using(config);
                    ctxl.batch(queries).execute();
                });
            }
        } catch (Exception ex) {
            log.error("Error executing queries as transaction with {} queries", queries.size(), ex).submit();
        }
    }

    public List<String> getPreviousNames(Client client) {
        List<String> names = new ArrayList<>();

        try {
            database.getDslContext().selectFrom(CLIENT_NAME_HISTORY)
                    .where(CLIENT_NAME_HISTORY.CLIENT.eq(client.getId()))
                    .fetch()
                    .forEach(nameRecord -> names.add(nameRecord.get(CLIENT_NAME_HISTORY.NAME)));
        } catch (Exception ex) {
            log.error("Error getting previous names for " + client.getName(), ex).submit();
        }

        return names;
    }

    public void updateClientName(Client client, String name) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.transaction(config -> {
                DSLContext ctxl = DSL.using(config);

                // Update client name
                ctxl.update(CLIENTS)
                        .set(CLIENTS.NAME, name)
                        .where(CLIENTS.UUID.eq(client.getUuid()))
                        .execute();

                // Prevent possibility of clients with same name
                // This happens if someone name changes and never joins server again, and then someone else takes their name
                ctxl.update(CLIENTS)
                        .set(CLIENTS.NAME, name + "-" + System.currentTimeMillis())
                        .where(CLIENTS.UUID.ne(client.getUuid()))
                        .and(CLIENTS.NAME.eq(name)).execute();

                // Insert old name into history if not already present
                ctxl.insertInto(CLIENT_NAME_HISTORY)
                        .set(CLIENT_NAME_HISTORY.CLIENT, client.getId())
                        .set(CLIENT_NAME_HISTORY.NAME, client.getName())
                        .set(CLIENT_NAME_HISTORY.LAST_SEEN, System.currentTimeMillis())
                        .onConflict(CLIENT_NAME_HISTORY.CLIENT, CLIENT_NAME_HISTORY.NAME)
                        .doNothing()
                        .execute();
            });
        });


    }

    public RewardBox getRewardBox(UUID id) {
        RewardBox rewardBox = new RewardBox();

        try {
            String rewards = database.getDslContext()
                    .select(CLIENT_REWARDS.REWARDS)
                    .from(CLIENT_REWARDS)
                    .where(CLIENT_REWARDS.CLIENT.eq(client.getId()))
                    .fetchOne(CLIENT_REWARDS.REWARDS);

            if (rewards == null) {
                rewards = UtilItem.serializeItemStackList(new ArrayList<>());
            }
            rewardBox.read(rewards);
        } catch (Exception ex) {
            log.error("Error getting rewards box for " + client.getName(), ex).submit();
            throw new RuntimeException(ex);
        }

        return rewardBox;
    }

    public CompletableFuture<Void> updateClientRewards(Client client, RewardBox rewardBox) {
        return database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            try {
                ctx.insertInto(CLIENT_REWARDS)
                        .set(CLIENT_REWARDS.CLIENT, client.getId())
                        .set(CLIENT_REWARDS.SEASON, Core.getCurrentSeason())
                        .set(CLIENT_REWARDS.REWARDS, rewardBox.serialize())
                        .onConflict(CLIENT_REWARDS.CLIENT, CLIENT_REWARDS.SEASON)
                        .doUpdate()
                        .set(CLIENT_REWARDS.REWARDS, rewardBox.serialize())
                        .execute();
            } catch (Exception ex) {
                log.error("Error updating rewards for " + client.getName(), ex).submit();
                throw new RuntimeException(ex);
            }
        });
    }


}
