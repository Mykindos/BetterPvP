package me.mykindos.betterpvp.core.combat.armour;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.SharedDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.repository.IRepository;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Singleton
@CustomLog
public class ArmourRepository implements IRepository<Armour> {

    private final SharedDatabase sharedDatabase;

    @Inject
    public ArmourRepository(SharedDatabase sharedDatabase) {
        this.sharedDatabase = sharedDatabase;
    }


    @Override
    public List<Armour> getAll() {
        List<Armour> armour = new ArrayList<>();
        String query = "SELECT * FROM armour";
        CachedRowSet result = sharedDatabase.executeQuery(new Statement(query));
        try {
            while (result.next()) {
                String type = result.getString(1);
                double reduction = result.getDouble(2);
                armour.add(new Armour(type, reduction));
            }
        } catch (SQLException ex) {
            log.error("Failed to load armour", ex);
        }
        return armour;
    }

    @Override
    public void save(Armour object) { // No saves done during runtime

    }
}
