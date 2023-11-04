package me.mykindos.betterpvp.core.combat.armour;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.repository.IRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class ArmourRepository implements IRepository<Armour> {

    @Inject
    @Config(path = "core.database.prefix")
    private String databasePrefix;

    private final Database database;

    @Inject
    public ArmourRepository(Database database) {
        this.database = database;
    }


    @Override
    public List<Armour> getAll() {
        List<Armour> armour = new ArrayList<>();
        String query = "SELECT * FROM " + databasePrefix + "armour";

        try (CachedRowSet result = database.executeQuery(new Statement(query))) {
            while (result.next()) {
                String type = result.getString(1);
                double reduction = result.getDouble(2);
                armour.add(new Armour(type, reduction));
            }
        } catch (SQLException ex) {
            log.error("Error loading armour values", ex);
        }
        return armour;
    }

    @Override
    public void save(Armour object) {

    }
}
