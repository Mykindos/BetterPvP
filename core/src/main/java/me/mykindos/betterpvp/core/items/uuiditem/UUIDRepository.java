package me.mykindos.betterpvp.core.items.uuiditem;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;

import javax.sql.rowset.CachedRowSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UUIDRepository implements IRepository<UUIDItem> {

    private Database database;

    @Inject
    UUIDRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<UUIDItem> getAll() {
        return null;
    }

    public List<UUIDItem> getUUIDItemsForModule(String namespace) {
        List<UUIDItem> UUIDItems = new ArrayList<>();
        String query = "SELECT * FROM uuiditems WHERE Namespace = ?";
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(namespace)));
        try {
            while (result.next()) {
                UUID uuid = UUID.fromString(result.getString(1));
                String key = result.getString(3);
                UUIDItems.add(new UUIDItem(uuid, namespace, key));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return UUIDItems;
    }

    @Override
    public void save (UUIDItem object) {
        String query = "INSERT INTO uuiditems (UUID, Namespace, Keyname) VALUES (?, ?, ?);";
        database.executeUpdate(new Statement(query,
                new UuidStatementValue(object.getUuid()),
                new StringStatementValue(object.getNamespace()),
                new StringStatementValue(object.getKey())
            )
        );
    }
}
