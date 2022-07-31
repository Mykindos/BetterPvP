package me.mykindos.betterpvp.core.client.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class ClientRepository implements IRepository<Client> {

    @Inject
    @Config(path = "core.database.prefix")
    private String databasePrefix;

    private final Database database;

    @Inject
    public ClientRepository(Database database) {
        this.database = database;
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
                    case "java.lang.Boolean" -> result.getBoolean(2);
                    default -> Class.forName(type).cast(result.getObject(2));
                };

                client.putProperty(value, property);
            }
        } catch (SQLException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void save(Client object) {
        String query = "INSERT INTO clients (UUID, Name) VALUES(?, ?);";
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(object.getUuid()),
                new StringStatementValue(object.getName())
        ));
    }
}
