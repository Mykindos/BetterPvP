package me.mykindos.betterpvp.core.items.uuiditem;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;

import javax.sql.rowset.CachedRowSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
@CustomLog
public class UUIDRepository implements IRepository<UUIDItem> {

    private final Database database;

    @Inject
    public UUIDRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<UUIDItem> getAll() {
        return new ArrayList<>();
    }

    public List<UUIDItem> getUUIDItemsForModule(String namespace) {
        List<UUIDItem> items = new ArrayList<>();
        String query = "SELECT * FROM uuiditems WHERE Namespace = ?";
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(namespace)));
        try {
            while (result.next()) {
                UUID uuid = UUID.fromString(result.getString(1));
                String key = result.getString(3);
                items.add(new UUIDItem(uuid, namespace, key));
            }
        } catch (Exception ex) {
            log.error("Failed to load UUIDItems for module: {}", namespace, ex);
        }
        return items;
    }

    @Override
    public void save(UUIDItem object) {
        String query = "INSERT INTO uuiditems (UUID, Namespace, Keyname) VALUES (?, ?, ?);";
        database.executeUpdate(new Statement(query,
                new UuidStatementValue(object.getUuid()),
                new StringStatementValue(object.getNamespace()),
                new StringStatementValue(object.getKey())
            )
        );
    }
}
