package me.mykindos.betterpvp.core.client.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class ClientRepository implements IRepository<Client> {

    @Inject
    @Config(path = "core.database.prefix")
    private String databasePrefix;

    private final Database database;

    private final ConcurrentHashMap<String, Statement> queuedStatUpdates;

    @Inject
    public ClientRepository(Database database) {
        this.database = database;
        this.queuedStatUpdates = new ConcurrentHashMap<>();
    }

    @Override
    public List<Client> getAll() {
        List<Client> clients = new ArrayList<>();
        String query = "SELECT * FROM clients;";
        CachedRowSet result = database.executeQuery(new Statement(query));
        try {
            while (result.next()) {
                String uuid = result.getString(2);
                String name = result.getString(3);
                Rank rank = Rank.valueOf(result.getString(4));

                Client client = Client.builder().uuid(uuid).name(name).rank(rank).build();
                loadProperties(client);
                clients.add(client);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        log.info("Loaded " + clients.size() + " clients");
        return clients;
    }

    private void loadProperties(Client client) {
        String query = "SELECT properties.Property, Value, Type FROM " + databasePrefix + "client_properties properties INNER JOIN "
                + "property_map map on properties.Property = map.Property WHERE Client = ?";
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(client.getUuid())));
        try {
            while (result.next()) {
                String value = result.getString(1);
                String type = result.getString(3);
                Object property = switch (type) {
                    case "java.lang.Integer" -> result.getInt(2);
                    case "java.lang.Boolean" -> Boolean.parseBoolean(result.getString(2));
                    default -> Class.forName(type).cast(result.getObject(2));
                };

                client.putProperty(ClientProperty.valueOf(value), property);
            }
        } catch (SQLException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void save(Client object) {
        String query = "INSERT INTO clients (UUID, Name) VALUES(?, ?) ON DUPLICATE KEY UPDATE `Rank` = ?;";
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(object.getUuid()),
                new StringStatementValue(object.getName()),
                new StringStatementValue(object.getRank().name())
        ));
    }

    public void saveProperty(Client client, Enum<?> property, Object value) {
        String savePropertyQuery = "INSERT INTO " + databasePrefix + "client_properties (Client, Property, Value) VALUES (?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE Value = ?";
        Statement statement = new Statement(savePropertyQuery,
                new StringStatementValue(client.getUuid()),
                new StringStatementValue(property.name()),
                new StringStatementValue(value.toString()),
                new StringStatementValue(value.toString()));
        queuedStatUpdates.put(client.getUuid() + property.name(), statement);
    }

    public void processStatUpdates(boolean async){
        ConcurrentHashMap<String, Statement> statements = new ConcurrentHashMap<>(queuedStatUpdates);
        queuedStatUpdates.clear();

        List<Statement> statementList = statements.values().stream().toList();
        database.executeBatch(statementList, async);

        log.info("Updated client stats with {} queries", statements.size());
    }
}
