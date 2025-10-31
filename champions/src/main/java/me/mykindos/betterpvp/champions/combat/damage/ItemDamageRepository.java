package me.mykindos.betterpvp.champions.combat.damage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CHAMPIONS_DAMAGEVALUES;

@Singleton
@CustomLog
public class ItemDamageRepository implements IRepository<ItemDamageValue> {

    private final Database database;

    @Inject
    public ItemDamageRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<ItemDamageValue> getAll() {
        List<ItemDamageValue> itemDamageValues = new ArrayList<>();
        try {
            database.getDslContext()
                    .selectFrom(CHAMPIONS_DAMAGEVALUES)
                    .fetch()
                    .forEach(dmgValueRecord -> {
                        Material item = Material.valueOf(dmgValueRecord.get(CHAMPIONS_DAMAGEVALUES.MATERIAL));
                        double damage = dmgValueRecord.get(CHAMPIONS_DAMAGEVALUES.DAMAGE);
                        itemDamageValues.add(new ItemDamageValue(item, damage));
                    });
        } catch (Exception ex) {
            log.error("Failed to load damage values", ex).submit();
        }

        return itemDamageValues;
    }

    @Override
    public void save(ItemDamageValue object) {

    }
}
