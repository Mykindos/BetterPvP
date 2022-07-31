package me.mykindos.betterpvp.clans.gamer.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.ObjectStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Singleton
public class GamerRepository implements IRepository<Gamer> {

    @Inject
    @Config(path = "clans.database.prefix")
    private String databasePrefix;

    private final Database database;
    private final ClientManager clientManager;

    @Inject
    public GamerRepository(Database database, ClientManager clientManager) {
        this.database = database;
        this.clientManager = clientManager;
    }

    @Override
    public List<Gamer> getAll() {
        List<Gamer> gamers = new ArrayList<>();
        String query = "SELECT * FROM " + databasePrefix + "gamers";
        CachedRowSet result = database.executeQuery(new Statement(query));
        try {
            while (result.next()) {
                String uuid = result.getString(2);

                Optional<Client> clientOptional = clientManager.getObject(uuid);
                clientOptional.ifPresent(client -> {
                    Gamer gamer = new Gamer(client, uuid);
                    loadProperties(gamer);
                    gamers.add(gamer);
                });


            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        log.info("Loaded " + gamers.size() + " gamers");
        return gamers;
    }

    private void loadProperties(Gamer gamer) {
        String query = "SELECT properties.Property, Value, Type FROM " + databasePrefix + "gamer_properties properties INNER JOIN "
                 + "property_map map on properties.Property = map.Property WHERE Gamer = ?";
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(gamer.getUuid())));
        try {
            while (result.next()) {
                String value = result.getString(1);
                String type = result.getString(3);
                Object property = switch (type) {
                    case "java.lang.Integer" -> result.getInt(2);
                    case "java.lang.Boolean" -> result.getBoolean(2);
                    default -> Class.forName(type).cast(result.getObject(2));
                };

                gamer.putProperty(value, property);
            }
        } catch (SQLException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }


    @Override
    public void save(Gamer gamer) {
        String query = "INSERT INTO " + databasePrefix + "gamers (UUID) VALUES(?);";
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(gamer.getUuid())
        ));

        gamer.getProperties().forEach((key, value) -> saveProperty(gamer, key, value));
    }

    public void saveProperty(Gamer gamer, String property, Object value) {
        String savePropertyQuery = "INSERT INTO " + databasePrefix + "gamer_properties (Gamer, Property, Value) VALUES (?, ?, ?)";
        database.executeUpdateAsync(new Statement(savePropertyQuery, new StringStatementValue(gamer.getUuid()),
                new StringStatementValue(property), new ObjectStatementValue(value)));
    }
}
