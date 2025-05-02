package me.mykindos.betterpvp.core.client.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.offlinemessages.OfflineMessagesRepository;
import me.mykindos.betterpvp.core.client.punishments.PunishmentRepository;
import me.mykindos.betterpvp.core.client.rewards.RewardBox;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.mappers.PropertyMapper;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
        String query = "SELECT * FROM clients WHERE UUID = ?;";
        CachedRowSet result = database.executeQuery(new Statement(query, new UuidStatementValue(uuid)), TargetDatabase.GLOBAL);
        try {
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
                client.getPunishments().addAll(punishmentRepository.getPunishmentsForClient(client));
                client.getIgnores().addAll(getIgnoresForClient(client));
                loadClientProperties(client);
                loadGamerProperties(client);
                return Optional.of(client);
            }
        } catch (SQLException ex) {
            log.error("Error loading client", ex).submit();
        }

        return Optional.empty();
    }

    public Optional<Client> getClient(@Nullable String name) {
        if (name == null) {
            return Optional.empty();
        }
        String query = "SELECT * FROM clients WHERE Name = ?;";
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(name)), TargetDatabase.GLOBAL);
        try {
            if (result.next()) {
                String uuid = result.getString(2);
                String actualName = result.getString(3);
                Rank rank = Rank.valueOf(result.getString(4));

                Gamer gamer = new Gamer(uuid);
                Client client = new Client(gamer, uuid, actualName, rank);
                client.getPunishments().addAll(punishmentRepository.getPunishmentsForClient(client));
                client.getIgnores().addAll(getIgnoresForClient(client));
                loadClientProperties(client);
                loadGamerProperties(client);
                return Optional.of(client);
            }
        } catch (SQLException ex) {
            log.error("Error loading client", ex).submit();
        }

        return Optional.empty();
    }

    private Set<UUID> getIgnoresForClient(Client client) {
        String query = "SELECT Ignored FROM ignores WHERE Client = ?;";
        CachedRowSet result = database.executeQuery(
                new Statement(query,
                        new StringStatementValue(client.getUuid())
                ), TargetDatabase.GLOBAL);
        HashSet<UUID> ignores = new HashSet<>();
        try {
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
        CachedRowSet result = database.executeQuery(new Statement(query), TargetDatabase.GLOBAL);
        try {
            if (result.next()) {
                return result.getInt(1);
            }
        } catch (SQLException ex) {
            log.error("Error fetching total clients", ex).submit();
        }

        return 0;
    }

    public void loadGamerProperties(Client client) {
        // Gamer
        Gamer gamer = client.getGamer();
        String query = "SELECT Property, Value FROM gamer_properties WHERE Gamer = ?";
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(gamer.getUuid())));
        try {
            propertyMapper.parseProperties(result, gamer);
        } catch (SQLException | ClassNotFoundException ex) {
            log.error("Failed to load gamer properties for {}", gamer.getUuid(), ex).submit();
        }
    }

    private void loadClientProperties(Client client) {
        // Client
        String query = "SELECT Property, Value FROM client_properties WHERE Client = ?";
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(client.getUuid())), TargetDatabase.GLOBAL);
        try {
            propertyMapper.parseProperties(result, client);
        } catch (SQLException | ClassNotFoundException ex) {
            log.error("Error loading client properties for " + client.getName(), ex).submit();
        }
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
                database.executeBatch(statements, async, TargetDatabase.GLOBAL);
            }
        }

        synchronized (queuedStatUpdates) {
            if (queuedStatUpdates.containsKey(uuid.toString())) {
                List<Statement> statements = queuedStatUpdates.remove(uuid.toString()).values().stream().toList();
                database.executeBatch(statements, async);
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

        database.executeBatch(statementsToRun, async, TargetDatabase.LOCAL);
        log.info("Updated gamer stats with {} queries", statementsToRun.size()).submit();


        // Client
        List<Statement> sharedStatementsToRun;
        synchronized (queuedSharedStatUpdates) {
            var sharedStatements = new ConcurrentHashMap<>(queuedSharedStatUpdates);
            sharedStatementsToRun = new ArrayList<>();
            sharedStatements.forEach((key, value) -> sharedStatementsToRun.addAll(value.values()));
            queuedSharedStatUpdates.clear();
        }

        database.executeBatch(sharedStatementsToRun, async, TargetDatabase.GLOBAL);
        log.info("Updated client stats with {} queries", sharedStatementsToRun.size()).submit();

    }

    public List<String> getAlts(Player player, String address) {
        List<String> alts = new ArrayList<>();
        String query = "SELECT DISTINCT l1.Value FROM logs_context l1 " +
                "INNER JOIN logs_context l2 ON l1.LogId = l2.LogId " +
                "WHERE l1.Context = 'ClientName' AND l2.Context = 'Address' AND l2.Value = ?";

        Statement statement = new Statement(query, new StringStatementValue(address));
        try (CachedRowSet result = database.executeQuery(statement)) {
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
        try (CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(client.getUuid())), TargetDatabase.GLOBAL)) {
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
        try (CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(client.getUuid())), TargetDatabase.GLOBAL)) {
            while (result.next()) {
                String data = result.getString(1);
                rewardBox.read(data);
            }
        } catch (SQLException ex) {
            log.error("Error getting rewards names for " + client.getName(), ex).submit();
        }

        return rewardBox;
    }

    public void updateClientRewards(Client client, RewardBox rewardBox) {
        String query = "UPDATE clients SET Rewards = ? WHERE uuid = ?;";
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(rewardBox.serialize()),
                new UuidStatementValue(client.getUniqueId())));
    }

}
