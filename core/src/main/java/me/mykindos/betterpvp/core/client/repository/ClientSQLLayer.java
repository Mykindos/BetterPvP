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
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.jooq.tables.records.ClientsRecord;
import me.mykindos.betterpvp.core.database.mappers.PropertyMapper;
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

import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENTS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENT_NAME_HISTORY;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENT_PROPERTIES;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENT_REWARDS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.GAMER_PROPERTIES;
import static me.mykindos.betterpvp.core.database.jooq.Tables.IGNORES;

@CustomLog
@Singleton
public class ClientSQLLayer {

    private final Database database;
    private final PropertyMapper propertyMapper;
    @Getter
    private final PunishmentRepository punishmentRepository;

    private final OfflineMessagesRepository offlineMessagesRepository;

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Query>> queuedStatUpdates;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Query>> queuedSharedStatUpdates;
    private static final ThreadLocal<Map<UUID, Client>> LOADING_CLIENTS = ThreadLocal.withInitial(HashMap::new);
    private static final SnowflakeIdGenerator ID_GENERATOR = new SnowflakeIdGenerator();

    @Inject
    public ClientSQLLayer(Database database, PropertyMapper propertyMapper, PunishmentRepository punishmentRepository, OfflineMessagesRepository offlineMessagesRepository) {
        this.database = database;
        this.propertyMapper = propertyMapper;
        this.punishmentRepository = punishmentRepository;
        this.offlineMessagesRepository = offlineMessagesRepository;
        this.queuedStatUpdates = new ConcurrentHashMap<>();
        this.queuedSharedStatUpdates = new ConcurrentHashMap<>();
    }

    public Client create(UUID uuid, String name) {
        long id = ID_GENERATOR.nextId();
        final Gamer gamer = new Gamer(id ,uuid.toString());
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
        return CompletableFuture.runAsync(() -> {
            Result<Record2<String, String>> result = database.getDslContext()
                    .select(CLIENT_PROPERTIES.PROPERTY, CLIENT_PROPERTIES.VALUE)
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
        return CompletableFuture.runAsync(() -> {
            Gamer gamer = client.getGamer();
            Result<Record2<String, String>> result = database.getDslContext()
                    .select(GAMER_PROPERTIES.PROPERTY, GAMER_PROPERTIES.VALUE)
                    .from(GAMER_PROPERTIES)
                    .where(GAMER_PROPERTIES.CLIENT.eq(client.getId()))
                    .and(GAMER_PROPERTIES.REALM.eq(Core.getCurrentRealm()))
                    .fetch();
            loadPropertiesAsync(result, gamer);

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

        return CompletableFuture.allOf(clientPropertiesFuture, gamerPropertiesFuture);
    }


    public void save(Client object) {
        database.getDslContext().insertInto(CLIENTS)
                .set(CLIENTS.ID, object.getId())
                .set(CLIENTS.UUID, object.getUuid())
                .set(CLIENTS.NAME, object.getName())
                .onConflict(CLIENTS.UUID)
                .doUpdate()
                .set(CLIENTS.NAME, object.getName())
                .set(CLIENTS.RANK, object.getRank().name())
                .execute();

        // Gamer
        final Gamer gamer = object.getGamer();
        gamer.getProperties().getMap().forEach((key, value) -> saveGamerProperty(gamer, key, value));
    }

    public void saveIgnore(Client client, Client ignored) {
        database.getDslContext().insertInto(IGNORES)
                .set(IGNORES.CLIENT, client.getId())
                .set(IGNORES.IGNORED, ignored.getId())
                .execute();
    }

    public void removeIgnore(Client client, Client ignored) {
        database.getDslContext().deleteFrom(IGNORES)
                .where(IGNORES.CLIENT.eq(client.getId()))
                .and(IGNORES.IGNORED.eq(ignored.getId()))
                .execute();
    }

    public void saveProperty(Client client, String property, Object value) {
        Query query = database.getDslContext().insertInto(CLIENT_PROPERTIES)
                .set(CLIENT_PROPERTIES.CLIENT, client.getId())
                .set(CLIENT_PROPERTIES.PROPERTY, property)
                .set(CLIENT_PROPERTIES.VALUE, value.toString())
                .onConflict(CLIENT_PROPERTIES.CLIENT, CLIENT_PROPERTIES.PROPERTY)
                .doUpdate()
                .set(CLIENT_PROPERTIES.VALUE, value.toString());

        ConcurrentHashMap<String, Query> propertyUpdates = queuedSharedStatUpdates.computeIfAbsent(client.getUuid(), k -> new ConcurrentHashMap<>());
        propertyUpdates.put(property, query);
        queuedSharedStatUpdates.put(client.getUuid(), propertyUpdates);
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

        ConcurrentHashMap<String, Query> propertyUpdates = queuedStatUpdates.computeIfAbsent(gamer.getUuid(), k -> new ConcurrentHashMap<>());
        propertyUpdates.put(property, query);

        queuedStatUpdates.put(gamer.getUuid(), propertyUpdates);
    }

    public void processStatUpdates(UUID uuid, boolean async) {
        synchronized (queuedStatUpdates) {
            if (queuedSharedStatUpdates.containsKey(uuid.toString())) {
                List<Query> queries = queuedSharedStatUpdates.remove(uuid.toString()).values().stream().toList();
                executeQueriesAsTransaction(queries);
            }
        }

        synchronized (queuedStatUpdates) {
            if (queuedStatUpdates.containsKey(uuid.toString())) {
                List<Query> queries = queuedStatUpdates.remove(uuid.toString()).values().stream().toList();
                executeQueriesAsTransaction(queries);
            }
        }

        log.info("Updated stats for {}", uuid).submit();
    }

    // There is a potential issue here where stat updates are cleared before they are processed due to aync processing
    public void processStatUpdates(boolean async) {

        log.info("Beginning to process stat updates").submit();

        // Gamer
        List<Query> statementsToRun;
        synchronized (queuedStatUpdates) {
            var statements = new ConcurrentHashMap<>(queuedStatUpdates);
            statementsToRun = new ArrayList<>();
            statements.forEach((key, value) -> statementsToRun.addAll(value.values()));
            queuedStatUpdates.clear();
        }

        executeQueriesAsTransaction(statementsToRun);
        log.info("Updated gamer stats with {} queries", statementsToRun.size()).submit();

        // Client
        List<Query> sharedStatementsToRun;
        synchronized (queuedSharedStatUpdates) {
            var sharedStatements = new ConcurrentHashMap<>(queuedSharedStatUpdates);
            sharedStatementsToRun = new ArrayList<>();
            sharedStatements.forEach((key, value) -> sharedStatementsToRun.addAll(value.values()));
            queuedSharedStatUpdates.clear();
        }

        executeQueriesAsTransaction(sharedStatementsToRun);
        log.info("Updated client stats with {} queries", sharedStatementsToRun.size()).submit();
    }

    /**
     * Executes a list of jOOQ queries as a single transaction.
     *
     * @param queries        The list of jOOQ Query objects to execute
     */
    private void executeQueriesAsTransaction(List<Query> queries) {
        if (queries == null || queries.isEmpty()) {
            return;
        }

        try {
            database.getAsyncDslContext().executeAsyncVoid(ctx -> {
                ctx.transaction(config -> {
                    DSLContext ctxl = DSL.using(config);
                    ctxl.batch(queries).execute();
                });
            });
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
        DSLContext ctx = database.getDslContext();

        // Update client name
        ctx.update(CLIENTS)
                .set(CLIENTS.NAME, name)
                .where(CLIENTS.UUID.eq(client.getUuid()))
                .execute();

        // Insert old name into history if not already present
        ctx.insertInto(CLIENT_NAME_HISTORY)
                .set(CLIENT_NAME_HISTORY.CLIENT, client.getId())
                .set(CLIENT_NAME_HISTORY.NAME, client.getName())
                .onConflict(CLIENT_NAME_HISTORY.CLIENT, CLIENT_NAME_HISTORY.NAME)
                .doNothing()
                .execute();
    }

    public RewardBox getRewardBox(Client client) {
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
        return CompletableFuture.runAsync(() -> {
            try {
                database.getDslContext().insertInto(CLIENT_REWARDS)
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
