package me.mykindos.betterpvp.core.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Material;

import javax.sql.rowset.CachedRowSet;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class ItemRepository implements IRepository<BPvPItem> {

    private Database database;

    @Inject
    public ItemRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<BPvPItem> getAll() {
        return null;
    }

    public List<BPvPItem> getItemsForModule(String namespace) {
        List<BPvPItem> items = new ArrayList<>();
        String query = "SELECT * FROM items WHERE Namespace = ?";
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(namespace)));
        try {
            while (result.next()) {
                int id = result.getInt(1);
                Material material = Material.getMaterial(result.getString(2));
                String key = result.getString(4);
                Component name = UtilMessage.deserialize(result.getString(5)).decoration(TextDecoration.ITALIC, false);
                int customModelData = result.getInt(6);
                boolean glowing = result.getBoolean(7);
                boolean uuid = result.getBoolean(8);

                List<Component> lore = getLoreForItem(id);
                int maxDurability = getMaxDurabilityForItem(id);

                if (material == null) {
                    log.info("Material is null for item {}", id);
                    continue;
                }

                items.add(new BPvPItem(namespace, key, material, name, lore, maxDurability, customModelData, glowing, uuid));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return items;
    }

    private List<Component> getLoreForItem(int id) {
        List<Component> lore = new ArrayList<>();
        String query = "SELECT * FROM itemlore WHERE Item = " + id + " ORDER BY Priority ASC";
        CachedRowSet result = database.executeQuery(new Statement(query));

        try {
            while (result.next()) {
                lore.add(UtilMessage.deserialize(result.getString(3)).decoration(TextDecoration.ITALIC, false));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return lore;
    }

    private int getMaxDurabilityForItem(int id) {
        String query = "SELECT * FROM itemdurability WHERE Item = " + id;
        CachedRowSet result = database.executeQuery(new Statement(query));

        try {
            while (result.next()) {
                return result.getInt(2);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    @Override
    public void save(BPvPItem object) {
        throw new NotImplementedException();
    }
}
