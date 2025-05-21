package me.mykindos.betterpvp.core.client.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.offlinemessages.OfflineMessagesRepository;
import me.mykindos.betterpvp.core.client.punishments.Punishment;
import me.mykindos.betterpvp.core.client.punishments.PunishmentRepository;
import me.mykindos.betterpvp.core.client.rewards.RewardBox;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.mappers.PropertyMapper;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

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
    @Getter
    private final PunishmentRepository punishmentRepository;

    private final OfflineMessagesRepository offlineMessagesRepository;

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Statement>> queuedStatUpdates;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Statement>> queuedSharedStatUpdates;
    private static final ThreadLocal<Map<UUID, Client>> LOADING_CLIENTS = ThreadLocal.withInitial(HashMap::new);

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
        final Gamer gamer = new Gamer(uuid.toString());
        final Client created = new Client(gamer, uuid.toString(), name, Rank.PLAYER);
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

        String query = "SELECT * FROM clients WHERE UUID = ?;";
        try (CachedRowSet result = database.executeQuery(new Statement(query, new UuidStatementValue(uuid)), TargetDatabase.GLOBAL).join()) {
            if (result.next()) {
                String name = result.getString(3);

                Rank rank = Rank.PLAYER;
                try {
                    rank = Rank.valueOf(result.getString(4));
                } catch (IllegalArgumentException ex) {
                    log.warn("Invalid rank for " + name + " (" + uuid + ")").submit();
                }

                Gamer gamer = new Gamer(uuid.toString());
                Client client = new Client(gamer, uuid.toString(), name, rank);
                loadingClients.put(uuid, client);

                loadAdditionalClientData(client);
                return Optional.of(client);
            }
        } catch (SQLException ex) {
            log.error("Error loading client", ex).submit();
        } finally {
            // Done loading, remove from loading map
            loadingClients.remove(uuid);
        }


        return Optional.empty();
    }

    public Optional<Client> getClient(@Nullable String name) {
        if (name == null) {
            return Optional.empty();
        }
        String query = "SELECT * FROM clients WHERE Name = ?;";
        try (CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(name)), TargetDatabase.GLOBAL).join()) {
            if (result.next()) {


                UUID uuid = UUID.fromString(result.getString(2));

                String actualName = result.getString(3);
                Rank rank = Rank.valueOf(result.getString(4));
                Gamer gamer = new Gamer(uuid.toString());
                Client client = new Client(gamer, uuid.toString(), actualName, rank);

                loadAdditionalClientData(client);
                return Optional.of(client);
            }
        } catch (SQLException ex) {
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
        String query = "SELECT Ignored FROM ignores WHERE Client = ?;";


        HashSet<UUID> ignores = new HashSet<>();
        try (CachedRowSet result = database.executeQuery(
                new Statement(query, new StringStatementValue(client.getUuid())), TargetDatabase.GLOBAL).join()) {
            while (result.next()) {
                String ignored = result.getString(1);
                ignores.add(UUID.fromString(ignored));
            }
        } catch (SQLException ex) {
            log.error("Error loading ignores {}", ex).submit();
        }

        return ignores;
    }

    public int getTotalClients() {
        String query = "SELECT COUNT(*) FROM clients;";

        try (CachedRowSet result = database.executeQuery(new Statement(query), TargetDatabase.GLOBAL).join()) {
            if (result.next()) {
                return result.getInt(1);
            }
        } catch (SQLException ex) {
            log.error("Error fetching total clients", ex).submit();
        }

        return 0;
    }

    /**
     * Loads properties for a given entity from the specified table concurrently.
     *
     * @param tableName         The database table to query
     * @param idColumnName      The name of the ID column in the table
     * @param idValue           The ID value to look up
     * @param propertyContainer The container to load properties into
     * @param targetDatabase    The database to query (LOCAL or GLOBAL)
     * @return A CompletableFuture that completes when the properties are loaded
     */
    private CompletableFuture<Void> loadPropertiesAsync(String tableName, String idColumnName, String idValue,
                                                        PropertyContainer propertyContainer, TargetDatabase targetDatabase) {
        String query = String.format("SELECT Property, Value FROM %s WHERE %s = ?",
                tableName, idColumnName);

        return database.executeQuery(new Statement(query, new StringStatementValue(idValue)), targetDatabase)
                .thenAccept(result -> {
                    try {
                        if (result != null) {
                            propertyMapper.parseProperties(result, propertyContainer);
                            result.close();
                        }
                    } catch (SQLException | ClassNotFoundException ex) {
                        log.error("Failed to load {} properties for {}",
                                tableName.split("_")[0], idValue, ex).submit();
                    }
                });
    }

    /**
     * Loads client properties asynchronously.
     *
     * @param client The client to load properties for
     * @return A CompletableFuture that completes when properties are loaded
     */
    public CompletableFuture<Void> loadClientPropertiesAsync(Client client) {
        return loadPropertiesAsync("client_properties", "Client", client.getUuid(), client, TargetDatabase.GLOBAL);
    }

    /**
     * Loads gamer properties asynchronously.
     *
     * @param client The client containing the gamer to load properties for
     * @return A CompletableFuture that completes when properties are loaded
     */
    public CompletableFuture<Void> loadGamerPropertiesAsync(Client client) {
        Gamer gamer = client.getGamer();
        return loadPropertiesAsync("gamer_properties", "Gamer", gamer.getUuid(), gamer, TargetDatabase.LOCAL);
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
        // Client
        String query = "INSERT INTO clients (UUID, Name) VALUES(?, ?) ON DUPLICATE KEY UPDATE Name = ?, `Rank` = ?;";
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(object.getUuid()),
                new StringStatementValue(object.getName()),
                new StringStatementValue(object.getName()),
                new StringStatementValue(object.getRank().name())
        ), TargetDatabase.GLOBAL);

        // Gamer
        final Gamer gamer = object.getGamer();
        gamer.getProperties().getMap().forEach((key, value) -> saveGamerProperty(gamer, key, value));

    }

    public void saveIgnore(Client client, Client ignored) {
        String update = "INSERT INTO ignores (Client, Ignored) VALUES (?, ?);";
        database.executeUpdateAsync(new Statement(update,
                new StringStatementValue(client.getUuid()),
                new StringStatementValue(ignored.getUuid())), TargetDatabase.GLOBAL);
    }

    public void removeIgnore(Client client, Client ignored) {
        String delete = "DELETE FROM ignores WHERE Client = ? AND Ignored = ?;";
        database.executeUpdateAsync(new Statement(delete,
                new StringStatementValue(client.getUuid()),
                new StringStatementValue(ignored.getUuid())), TargetDatabase.GLOBAL);
    }

    public void saveProperty(Client client, String property, Object value) {
        // Client
        String savePropertyQuery = "INSERT INTO client_properties (Client, Property, Value) VALUES (?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE Value = ?";
        Statement statement = new Statement(savePropertyQuery,
                new StringStatementValue(client.getUuid()),
                new StringStatementValue(property),
                new StringStatementValue(value.toString()),
                new StringStatementValue(value.toString()));

        ConcurrentHashMap<String, Statement> propertyUpdates = queuedSharedStatUpdates.computeIfAbsent(client.getUuid(), k -> new ConcurrentHashMap<>());
        propertyUpdates.put(property, statement);
        queuedSharedStatUpdates.put(client.getUuid(), propertyUpdates);
    }

    public void saveGamerProperty(Gamer gamer, String property, Object value) {
        // Gamer
        String savePropertyQuery = "INSERT INTO gamer_properties (Gamer, Property, Value) VALUES (?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE Value = ?";

        Statement statement = new Statement(savePropertyQuery,
                new StringStatementValue(gamer.getUuid()),
                new StringStatementValue(property),
                new StringStatementValue(value.toString()),
                new StringStatementValue(value.toString()));

        ConcurrentHashMap<String, Statement> propertyUpdates = queuedStatUpdates.computeIfAbsent(gamer.getUuid(), k -> new ConcurrentHashMap<>());
        propertyUpdates.put(property, statement);

        queuedStatUpdates.put(gamer.getUuid(), propertyUpdates);
    }

    public void processStatUpdates(UUID uuid, boolean async) {
        synchronized (queuedStatUpdates) {
            if (queuedSharedStatUpdates.containsKey(uuid.toString())) {
                List<Statement> statements = queuedSharedStatUpdates.remove(uuid.toString()).values().stream().toList();
                database.executeBatch(statements, TargetDatabase.GLOBAL);
            }
        }

        synchronized (queuedStatUpdates) {
            if (queuedStatUpdates.containsKey(uuid.toString())) {
                List<Statement> statements = queuedStatUpdates.remove(uuid.toString()).values().stream().toList();
                database.executeBatch(statements);
            }
        }

        log.info("Updated stats for {}", uuid).submit();
    }

    // There is a potential issue here where stat updates are cleared before they are processed due to aync processing
    public void processStatUpdates(boolean async) {

        log.info("Beginning to process stat updates").submit();

        // Gamer
        List<Statement> statementsToRun;
        synchronized (queuedStatUpdates) {
            var statements = new ConcurrentHashMap<>(queuedStatUpdates);
            statementsToRun = new ArrayList<>();
            statements.forEach((key, value) -> statementsToRun.addAll(value.values()));
            queuedStatUpdates.clear();
        }

        database.executeBatch(statementsToRun, TargetDatabase.LOCAL);
        log.info("Updated gamer stats with {} queries", statementsToRun.size()).submit();


        // Client
        List<Statement> sharedStatementsToRun;
        synchronized (queuedSharedStatUpdates) {
            var sharedStatements = new ConcurrentHashMap<>(queuedSharedStatUpdates);
            sharedStatementsToRun = new ArrayList<>();
            sharedStatements.forEach((key, value) -> sharedStatementsToRun.addAll(value.values()));
            queuedSharedStatUpdates.clear();
        }

        database.executeBatch(sharedStatementsToRun, TargetDatabase.GLOBAL);
        log.info("Updated client stats with {} queries", sharedStatementsToRun.size()).submit();

    }

    public List<String> getAlts(Player player, String address) {
        List<String> alts = new ArrayList<>();
        String query = "SELECT DISTINCT l1.Value FROM logs_context l1 " +
                "INNER JOIN logs_context l2 ON l1.LogId = l2.LogId " +
                "WHERE l1.Context = 'ClientName' AND l2.Context = 'Address' AND l2.Value = ?";

        Statement statement = new Statement(query, new StringStatementValue(address));
        try (CachedRowSet result = database.executeQuery(statement).join()) {
            while (result.next()) {
                String name = result.getString(1);
                if (!name.equals(player.getName())) {
                    alts.add(name);
                }
            }
        } catch (SQLException ex) {
            log.error("Error getting alts for " + player.getName(), ex).submit();
        }

        return alts;
    }

    public List<String> getPreviousNames(Client client) {
        List<String> names = new ArrayList<>();
        String query = "SELECT Name FROM client_name_history WHERE Client = ?;";
        try (CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(client.getUuid())), TargetDatabase.GLOBAL).join()) {
            while (result.next()) {
                String name = result.getString(1);
                names.add(name);

            }
        } catch (SQLException ex) {
            log.error("Error getting previous names for " + client.getName(), ex).submit();
        }

        return names;
    }

    public void updateClientName(Client client, String name) {
        String query = "UPDATE clients SET Name = ? WHERE UUID = ?;";
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(name),
                new StringStatementValue(client.getUuid())), TargetDatabase.GLOBAL);

        String oldNameQuery = "INSERT IGNORE INTO client_name_history (Client, Name) VALUES (?, ?);";
        database.executeUpdateAsync(new Statement(oldNameQuery,
                new StringStatementValue(client.getUuid()),
                new StringStatementValue(client.getName())), TargetDatabase.GLOBAL);
    }

    public RewardBox getRewardBox(Client client) {
        RewardBox rewardBox = new RewardBox();

        String query = "SELECT Rewards FROM clients WHERE UUID = ?;";
        try (CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(client.getUuid())), TargetDatabase.GLOBAL).join()) {
            while (result.next()) {
                String data = result.getString(1);
                if (data == null) {
                    data = UtilItem.serializeItemStackList(new ArrayList<>());
                }
                rewardBox.read(data);
            }
        } catch (SQLException ex) {
            log.error("Error getting rewards box for " + client.getName(), ex).submit();
            throw new RuntimeException(ex);
        }

        return rewardBox;
    }

    public CompletableFuture<Void> updateClientRewards(Client client, RewardBox rewardBox) {
        String query = "UPDATE clients SET Rewards = ? WHERE UUID = ?;";
        return database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(rewardBox.serialize()),
                new UuidStatementValue(client.getUniqueId())), TargetDatabase.GLOBAL);
    }

}
