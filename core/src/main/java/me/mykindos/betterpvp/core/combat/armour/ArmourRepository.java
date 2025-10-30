package me.mykindos.betterpvp.core.combat.armour;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.repository.IRepository;

import java.util.ArrayList;
import java.util.List;

import static me.mykindos.betterpvp.core.database.jooq.Tables.ARMOUR;

@Singleton
@CustomLog
public class ArmourRepository implements IRepository<Armour> {

    private final Database database;

    @Inject
    public ArmourRepository(Database database) {
        this.database = database;
    }


    @Override
    public List<Armour> getAll() {
        List<Armour> armour = new ArrayList<>();

        try {
            database.getDslContext()
                    .selectFrom(ARMOUR)
                    .fetch()
                    .forEach(armourRecord -> {
                        String type = armourRecord.get(ARMOUR.ITEM);
                        double reduction = armourRecord.get(ARMOUR.REDUCTION);
                        armour.add(new Armour(type, reduction));
                    });
        } catch (Exception ex) {
            log.error("Failed to load armour", ex);
        }
        return armour;
    }

    @Override
    public void save(Armour object) { // No saves done during runtime

    }
}
