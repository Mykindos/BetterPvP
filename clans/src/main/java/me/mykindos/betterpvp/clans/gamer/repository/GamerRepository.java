package me.mykindos.betterpvp.clans.gamer.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class GamerRepository implements IRepository<Gamer> {

    @Inject
    @Config(path="database.prefix")
    private String databasePrefix;

    private Database database;
    private ClientManager clientManager;

    @Inject
    public GamerRepository(Database database, ClientManager clientManager){
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
                clientOptional.ifPresent(client -> gamers.add(new Gamer(client, uuid)));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        System.out.println("Loaded " + gamers.size() + " gamers");
        return gamers;
    }

    @Override
    public void save(Gamer object) {
        String query = "INSERT INTO " + databasePrefix + "gamers (UUID) VALUES(?);";
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(object.getUuid())
        ));
    }
}
