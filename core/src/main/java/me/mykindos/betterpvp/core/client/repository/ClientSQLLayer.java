package me.mykindos.betterpvp.core.client.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.SharedDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class ClientSQLLayer {

    private final Database database;
    private final SharedDatabase sharedDatabase;
    private final Map<String, String> propertyMap;

    private final ConcurrentHashMap<String, Statement> queuedStatUpdates;
    private final ConcurrentHashMap<String, Statement> queuedSharedStatUpdates;

    @Inject
    public ClientSQLLayer(Database database, SharedDatabase sharedDatabase) {
        this.database = database;
        this.sharedDatabase = sharedDatabase;
        this.queuedStatUpdates = new ConcurrentHashMap<>();
        this.queuedSharedStatUpdates = new ConcurrentHashMap<>();
        this.propertyMap = this.getPropertyMap();
    }

    public Client create(UUID uuid, String name) {
        final Gamer gamer = new Gamer(uuid.toString());
        final Client created = new Client(gamer, uuid.toString(), name, Rank.PLAYER);
        save(created);
        created.setNewClient(true);
        return created;
    }

    public Optional<Client> getAndUpdate(UUID uuid, String name) {
        final Optional<Client> client = getClient(uuid);
        client.ifPresent(loaded -> {
            if (!loaded.getName().equals(name)) {
                loaded.setName(name);
                save(loaded);
            }
        });
        return client;
    }

    public Optional<Client> getClient(UUID uuid) {
        String query = "SELECT * FROM clients WHERE UUID = ?;";
        CachedRowSet result = sharedDatabase.executeQuery(new Statement(query, new UuidStatementValue(uuid)));
        try {
            if (result.next()) {
                String name = result.getString(3);
                Rank rank = Rank.valueOf(result.getString(4));

                Gamer gamer = new Gamer(uuid.toString());
                Client client = new Client(gamer, uuid.toString(), name, rank);
                loadClientProperties(client);
                return Optional.of(client);
            }
        } catch (SQLException ex) {
            log.error("Error loading client", ex);
        }

        return Optional.empty();
    }

    public Optional<Client> getClient(String name) {
        String query = "SELECT * FROM clients WHERE Name = ?;";
        CachedRowSet result = sharedDatabase.executeQuery(new Statement(query, new StringStatementValue(name)));
        try {
            if (result.next()) {
                String uuid = result.getString(2);
                Rank rank = Rank.valueOf(result.getString(4));

                Gamer gamer = new Gamer(uuid);
                Client client = new Client(gamer, uuid, name, rank);
                loadClientProperties(client);
                return Optional.of(client);
            }
        } catch (SQLException ex) {
            log.error("Error loading client", ex);
        }

        return Optional.empty();
    }

    public void loadGamerProperties(Client client) {
        // Gamer
        Gamer gamer = client.getGamer();
        String query2 = "SELECT Property, Value FROM gamer_properties WHERE Gamer = ?";
        CachedRowSet result2 = database.executeQuery(new Statement(query2, new StringStatementValue(gamer.getUuid())));
        try {
            while (result2.next()) {
                String property = result2.getString(1);
                String type = propertyMap.get(property);
                Object value = switch (type) {
                    case "int" -> result2.getInt(2);
                    case "boolean" -> Boolean.parseBoolean(result2.getString(2));
                    case "double" -> Double.parseDouble(result2.getString(2));
                    default -> Class.forName(type).cast(result2.getObject(2));
                };

                gamer.putProperty(property, value, true);
            }
        } catch (SQLException | ClassNotFoundException ex) {
            log.error("Failed to load gamer properties for {}", gamer.getUuid(), ex);
        }
    }

    private void loadClientProperties(Client client) {
        // Client
        String query = "SELECT Property, Value FROM client_properties WHERE Client = ?";
        CachedRowSet result = sharedDatabase.executeQuery(new Statement(query, new StringStatementValue(client.getUuid())));
        try {
            while (result.next()) {
                String property = result.getString(1);
                String type = propertyMap.get(property);
                Object value = switch (type) {
                    case "int" -> result.getInt(2);
                    case "boolean" -> Boolean.parseBoolean(result.getString(2));
                    case "double" -> Double.parseDouble(result.getString(2));
                    default -> Class.forName(type).cast(result.getObject(2));
                };

                client.putProperty(property, value, true);
            }
        } catch (SQLException | ClassNotFoundException ex) {
            log.error("Error loading client properties for " + client.getName(), ex);
        }
    }

    public void save(Client object) {
        // Client
        String query = "INSERT INTO clients (UUID, Name) VALUES(?, ?) ON DUPLICATE KEY UPDATE `Rank` = ?;";
        sharedDatabase.executeUpdateAsync(new Statement(query,
                new StringStatementValue(object.getUuid()),
                new StringStatementValue(object.getName()),
                new StringStatementValue(object.getRank().name())
        ));

        // Gamer
        final Gamer gamer = object.getGamer();
        gamer.getProperties().getMap().forEach((key, value) -> saveGamerProperty(gamer, key, value));
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
        queuedSharedStatUpdates.put(client.getUuid() + property, statement);
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
        queuedStatUpdates.put(gamer.getUuid() + property, statement);
    }

    public void processStatUpdates(boolean async) {
        // Client
        ConcurrentHashMap<String, Statement> statements = new ConcurrentHashMap<>(queuedSharedStatUpdates);
        queuedSharedStatUpdates.clear();
        sharedDatabase.executeBatch(statements.values().stream().toList(), async);

        // Gamer
        statements = new ConcurrentHashMap<>(queuedStatUpdates);
        queuedStatUpdates.clear();
        database.executeBatch(statements.values().stream().toList(), async);

        log.info("Updated client and gamer stats with {} queries", statements.size());
    }

    public Map<String, String> getPropertyMap() {
        String query = "SELECT * FROM property_map;";
        CachedRowSet result = sharedDatabase.executeQuery(new Statement(query));
        Map<String, String> map = new ConcurrentHashMap<>();
        try {
            while (result.next()) {
                map.put(result.getString(1), result.getString(2));
            }
        } catch (SQLException ex) {
            log.error("Error loading property map", ex);
        }

        return map;
    }
}
