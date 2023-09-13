package me.mykindos.betterpvp.champions.crafting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import org.bukkit.Material;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Singleton
public class ImbuementRepository implements IRepository<Imbuement> {

    @Inject
    @Config(path = "champions.database.prefix", defaultValue = "champions_")
    private String databasePrefix;

    private final Database database;

    @Inject
    public ImbuementRepository(Database database) {
        this.database = database;
    }


    public List<Imbuement> getAll() {
        List<Imbuement> imbuementValues = new ArrayList<>();
        String query = "SELECT * FROM " + databasePrefix + "imbuement_data;";
        CachedRowSet result = database.executeQuery(new Statement(query));
        try {
            while (result.next()) {
                String name = result.getString(1);
                String key = result.getString(2);
                String affixText = result.getString(3);
                Material material = Material.valueOf(result.getString(4));
                boolean canImbueArmour = result.getBoolean(5);
                boolean canImbueWeapons = result.getBoolean(6);
                boolean canImbueTools = result.getBoolean(7);

                var imbuement = new Imbuement(name, key, affixText, material, canImbueArmour, canImbueWeapons, canImbueTools);

                imbuementValues.add(imbuement);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return imbuementValues;
    }

    @Override
    public void save(Imbuement object) {

    }

}
