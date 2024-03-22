package me.mykindos.betterpvp.champions.combat.damage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import org.bukkit.Material;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
        String query = "SELECT * FROM champions_damagevalues;";
        CachedRowSet result = database.executeQuery(new Statement(query));
        try {
            while (result.next()) {
                Material item = Material.valueOf(result.getString(1));
                double damage = result.getDouble(2);
                itemDamageValues.add(new ItemDamageValue(item, damage));
            }
        } catch (SQLException ex) {
            log.error("Failed to load damage values", ex);
        }

        return itemDamageValues;
    }

    @Override
    public void save(ItemDamageValue object) {

    }
}
