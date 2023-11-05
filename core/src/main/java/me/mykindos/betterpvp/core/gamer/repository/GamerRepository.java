package me.mykindos.betterpvp.core.gamer.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.gamer.Gamer;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class GamerRepository implements IRepository<Gamer> {

    @Inject
    @Getter
    @Config(path = "core.database.prefix")
    private String databasePrefix;

    private final Database database;
    private final ClientManager clientManager;

    private final ConcurrentHashMap<String, Statement> queuedStatUpdates;

    @Inject
    public GamerRepository(Database database, ClientManager clientManager) {
        this.database = database;
        this.clientManager = clientManager;
        queuedStatUpdates = new ConcurrentHashMap<>();
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
            log.error("Failed to load gamers", ex);
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
                    case "int" -> result.getInt(2);
                    case "boolean" -> Boolean.parseBoolean(result.getString(2));
                    case "double" -> Double.parseDouble(result.getString(2));
                    default -> Class.forName(type).cast(result.getObject(2));
                };

                gamer.putProperty(value, property, true);
            }
        } catch (SQLException | ClassNotFoundException ex) {
            log.error("Failed to load gamer properties for {}", gamer.getUuid(), ex);
        }
    }


    @Override
    public void save(Gamer gamer) {
        String query = "INSERT INTO " + databasePrefix + "gamers (UUID) VALUES(?);";
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(gamer.getUuid())
        ));

        gamer.getProperties().getMap().forEach((key, value) -> saveProperty(gamer, key, value));
    }

    public void saveProperty(Gamer gamer, String property, Object value) {
        String savePropertyQuery = "INSERT INTO " + databasePrefix + "gamer_properties (Gamer, Property, Value) VALUES (?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE Value = ?";

        Statement statement = new Statement(savePropertyQuery,
                new StringStatementValue(gamer.getUuid()),
                new StringStatementValue(property),
                new StringStatementValue(value.toString()),
                new StringStatementValue(value.toString()));
        queuedStatUpdates.put(gamer.getUuid() + property, statement);
    }

    public void processStatUpdates(boolean async) {
        ConcurrentHashMap<String, Statement> statements = new ConcurrentHashMap<>(queuedStatUpdates);
        queuedStatUpdates.clear();

        List<Statement> statementList = statements.values().stream().toList();
        database.executeBatch(statementList, async);

        log.info("Updated gamer stats with {} queries", statements.size());
    }

}
