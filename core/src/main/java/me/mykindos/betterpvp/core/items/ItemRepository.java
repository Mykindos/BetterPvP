package me.mykindos.betterpvp.core.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.NotImplementedException;
import org.bukkit.Material;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.List;

import static me.mykindos.betterpvp.core.database.jooq.Tables.ITEMDURABILITY;
import static me.mykindos.betterpvp.core.database.jooq.Tables.ITEMLORE;
import static me.mykindos.betterpvp.core.database.jooq.Tables.ITEMS;

@Singleton
@CustomLog
public class ItemRepository implements IRepository<BPvPItem> {

    private final Database database;

    @Inject
    public ItemRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<BPvPItem> getAll() {
        return new ArrayList<>();
    }

    public List<BPvPItem> getItemsForModule(String namespace) {
        List<BPvPItem> items = new ArrayList<>();
        try {
            DSLContext ctx = database.getDslContext();

            ctx.selectFrom(ITEMS)
                    .where(ITEMS.NAMESPACE.eq(namespace))
                    .fetch()
                    .forEach(itemRecord -> {
                        int id = itemRecord.get(ITEMS.ID);
                        Material material = Material.getMaterial(itemRecord.get(ITEMS.MATERIAL));
                        String key = itemRecord.get(ITEMS.KEYNAME);
                        Component name = UtilMessage.deserialize(itemRecord.get(ITEMS.NAME))
                                .decoration(TextDecoration.ITALIC, false);
                        int customModelData = itemRecord.get(ITEMS.MODEL_DATA);
                        boolean glowing = itemRecord.get(ITEMS.GLOW) == 1;
                        boolean uuid = itemRecord.get(ITEMS.HAS_UUID) == 1;

                        List<Component> lore = getLoreForItem(id);
                        int maxDurability = getMaxDurabilityForItem(id);

                        if (material == null) {
                            log.info("Material is null for item {}", id).submit();
                            return;
                        }

                        items.add(new BPvPItem(namespace, key, material, name, lore, maxDurability,
                                customModelData, glowing, uuid));
                    });
        } catch (Exception ex) {
            log.error("Failed to load items for module {}", namespace, ex).submit();
        }

        return items;
    }

    private List<Component> getLoreForItem(int id) {
        List<Component> lore = new ArrayList<>();
        try {
            DSLContext ctx = database.getDslContext();

            ctx.selectFrom(ITEMLORE)
                    .where(ITEMLORE.ITEM.eq(id))
                    .orderBy(ITEMLORE.PRIORITY.asc())
                    .fetch()
                    .forEach(loreRecord -> {
                        lore.add(UtilMessage.deserialize(loreRecord.get(ITEMLORE.TEXT))
                                .decoration(TextDecoration.ITALIC, false));
                    });
        } catch (Exception ex) {
            log.error("Failed to load lore for item {}", id, ex).submit();
        }


        return lore;
    }

    private int getMaxDurabilityForItem(int id) {
        try {
            DSLContext ctx = database.getDslContext();

            Integer maxDurability = ctx.selectFrom(ITEMDURABILITY)
                    .where(ITEMDURABILITY.ITEM.eq(id))
                    .fetchOne(ITEMDURABILITY.DURABILITY);

            return maxDurability != null ? maxDurability : -1;
        } catch (Exception ex) {
            log.error("Failed to load max durability for item {}", id, ex).submit();
        }
        return -1;
    }

    @Override
    public void save(BPvPItem object) {
        throw new NotImplementedException("ItemRepository does not support saving items");
    }
}
