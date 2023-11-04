package me.mykindos.betterpvp.champions.combat.damage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import org.bukkit.Material;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class ItemDamageRepository implements IRepository<ItemDamageValue> {

    @Inject
    @Config(path = "champions.database.prefix", defaultValue = "champions_")
    private String databasePrefix;

    private final Database database;

    @Inject
    public ItemDamageRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<ItemDamageValue> getAll() {
        List<ItemDamageValue> itemDamageValues = new ArrayList<>();
        String query = "SELECT * FROM " + databasePrefix + "damagevalues;";

        try (CachedRowSet result = database.executeQuery(new Statement(query))) {
            while (result.next()) {
                Material item = Material.valueOf(result.getString(1));
                double damage = result.getDouble(2);
                itemDamageValues.add(new ItemDamageValue(item, damage));
            }
        } catch (SQLException ex) {
            log.error("Error loading damage values", ex);
        }

        return itemDamageValues;
    }

    @Override
    public void save(ItemDamageValue object) {

    }
}
