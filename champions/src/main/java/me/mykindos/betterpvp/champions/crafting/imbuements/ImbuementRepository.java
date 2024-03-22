package me.mykindos.betterpvp.champions.crafting.imbuements;

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
public class ImbuementRepository implements IRepository<Imbuement> {

    private final Database database;

    @Inject
    public ImbuementRepository(Database database) {
        this.database = database;
    }


    public List<Imbuement> getAll() {
        List<Imbuement> imbuementValues = new ArrayList<>();
        String query = "SELECT * FROM champions_imbuement_data;";
        CachedRowSet result = database.executeQuery(new Statement(query));
        try {
            while (result.next()) {
                String name = result.getString(1);
                String key = result.getString(2);
                String affixText = result.getString(3);
                Material material = Material.valueOf(result.getString(4));
                double value = result.getDouble(5);
                boolean canImbueArmour = result.getBoolean(6);
                boolean canImbueWeapons = result.getBoolean(7);
                boolean canImbueTools = result.getBoolean(8);

                var imbuement = new Imbuement(name, key, affixText, material, value, canImbueArmour, canImbueWeapons, canImbueTools);

                imbuementValues.add(imbuement);
            }
        } catch (SQLException ex) {
            log.error("Failed to load imbuement values", ex);
        }

        return imbuementValues;
    }

    @Override
    public void save(Imbuement object) {

    }

}
