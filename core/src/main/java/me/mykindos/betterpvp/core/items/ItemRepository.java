package me.mykindos.betterpvp.core.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.Config;
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
public class ItemRepository implements IRepository<BPVPItem> {

    @Inject
    @Config(path = "core.database.prefix")
    private String databasePrefix;
    private Database database;

    @Inject
    public ItemRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<BPVPItem> getAll() {
        return null;
    }

    public List<BPVPItem> getItemsForModule(String module) {
        List<BPVPItem> items = new ArrayList<>();
        String query = "SELECT * FROM " + databasePrefix + "items WHERE Module = ?";
        CachedRowSet result = database.executeQuery(new Statement(query, new StringStatementValue(module)));
        try {
            while (result.next()) {
                int id = result.getInt(1);
                Material material = Material.getMaterial(result.getString(2));
                Component name = UtilMessage.deserialize(result.getString(4)).decoration(TextDecoration.ITALIC, false);
                int customModelData = result.getInt(5);
                boolean glowing = result.getBoolean(6);
                boolean uuid = result.getBoolean(7);

                List<Component> lore = getLoreForItem(id);

                if (material == null) {
                    log.info("Material is null for item {}", id);
                    continue;
                }

                items.add(new BPVPItem(material, name, lore, customModelData, glowing, uuid));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return items;
    }

    private List<Component> getLoreForItem(int id) {
        List<Component> lore = new ArrayList<>();
        String query = "SELECT * FROM " + databasePrefix + "itemlore WHERE Item = " + id + " ORDER BY Priority ASC";
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

    @Override
    public void save(BPVPItem object) {
        throw new NotImplementedException();
    }
}
