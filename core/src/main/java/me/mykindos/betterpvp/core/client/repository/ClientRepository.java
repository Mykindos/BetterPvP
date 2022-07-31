package me.mykindos.betterpvp.core.client.repository;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public record ClientRepository(Database database) implements IRepository<Client> {

    @Inject
    public ClientRepository {
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
                clients.add(client);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        log.info("Loaded " + clients.size() + " clients");
        return clients;
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
