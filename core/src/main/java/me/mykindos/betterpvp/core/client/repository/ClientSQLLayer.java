package me.mykindos.betterpvp.core.client.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class ClientSQLLayer {

    private final Database database;

    private final ConcurrentHashMap<String, Statement> queuedStatUpdates;

    @Inject
    public ClientSQLLayer(Database database) {
        this.database = database;
        this.queuedStatUpdates = new ConcurrentHashMap<>();
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
        CachedRowSet result = database.executeQuery(new Statement(query, new UuidStatementValue(uuid)));
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
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(name)));
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
        String query2 = "SELECT properties.Property, Value, Type FROM gamer_properties properties INNER JOIN "
                + "property_map map on properties.Property = map.Property WHERE Gamer = ?";
        CachedRowSet result2 = database.executeQuery(new Statement(query2, new StringStatementValue(gamer.getUuid())));
        try {
            while (result2.next()) {
                String value = result2.getString(1);
                String type = result2.getString(3);
                Object property = switch (type) {
                    case "int" -> result2.getInt(2);
                    case "boolean" -> Boolean.parseBoolean(result2.getString(2));
                    case "double" -> Double.parseDouble(result2.getString(2));
                    default -> Class.forName(type).cast(result2.getObject(2));
                };

                gamer.putProperty(value, property, true);
            }
        } catch (SQLException | ClassNotFoundException ex) {
            log.error("Failed to load gamer properties for {}", gamer.getUuid(), ex);
        }
    }

    private void loadClientProperties(Client client) {
        // Client
        String query = "SELECT properties.Property, Value, Type FROM client_properties properties INNER JOIN "
                + "property_map map on properties.Property = map.Property WHERE Client = ?";
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(client.getUuid())));
        try {
            while (result.next()) {
                String value = result.getString(1);
                String type = result.getString(3);
                Object property = switch (type) {
                    case "int" -> result.getInt(2);
                    case "boolean" -> Boolean.parseBoolean(result.getString(2));
                    case "double" -> Double.parseDouble(result.getString(2));
                    default -> Class.forName(type).cast(result.getObject(2));
                };

                client.putProperty(value, property, true);
            }
        } catch (SQLException | ClassNotFoundException ex) {
            log.error("Error loading client properties for " + client.getName(), ex);
        }
    }

    public void save(Client object) {
        // Client
        String query = "INSERT INTO clients (UUID, Name) VALUES(?, ?) ON DUPLICATE KEY UPDATE `Rank` = ?;";
        database.executeUpdateAsync(new Statement(query,
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
        queuedStatUpdates.put(client.getUuid() + property, statement);
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

    public void processStatUpdates(boolean async){
        // Client & Gamer
        ConcurrentHashMap<String, Statement> statements = new ConcurrentHashMap<>(queuedStatUpdates);
        queuedStatUpdates.clear();

        List<Statement> statementList = statements.values().stream().toList();
        database.executeBatch(statementList, async);

        log.info("Updated client and gamer stats with {} queries", statements.size());
    }

}
