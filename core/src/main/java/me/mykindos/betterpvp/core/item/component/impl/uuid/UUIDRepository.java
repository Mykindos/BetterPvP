package me.mykindos.betterpvp.core.item.component.impl.uuid;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.repository.IRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static me.mykindos.betterpvp.core.database.jooq.Tables.UUIDITEMS;

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

        try {
            database.getDslContext()
                    .selectFrom(UUIDITEMS)
                    .where(UUIDITEMS.NAMESPACE.eq(namespace))
                    .and(UUIDITEMS.REALM.eq(Core.getCurrentRealm().getRealm()))
                    .fetch()
                    .forEach(uuidItemRecord -> {
                        UUID uuid = UUID.fromString(uuidItemRecord.get(UUIDITEMS.UUID));
                        String key = uuidItemRecord.get(UUIDITEMS.KEYNAME);
                        items.add(new UUIDItem(uuid, namespace, key));
                    });
        } catch (Exception ex) {
            log.error("Failed to load UUIDItems for module: {}", namespace, ex).submit();
        }

        return items;
    }

    @Override
    public void save(UUIDItem object) {
        try {
            database.getDslContext()
                    .insertInto(UUIDITEMS)
                    .set(UUIDITEMS.UUID, object.getUuid().toString())
                    .set(UUIDITEMS.REALM, Core.getCurrentRealm().getRealm())
                    .set(UUIDITEMS.NAMESPACE, object.getNamespace())
                    .set(UUIDITEMS.KEYNAME, object.getKey())
                    .execute();
        } catch (Exception ex) {
            log.error("Failed to save UUIDItem for namespace: {}", object.getNamespace(), ex).submit();
        }
    }
}