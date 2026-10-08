package me.mykindos.betterpvp.core.scene.mob;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Husk;
import org.jetbrains.annotations.NotNull;

/** Spawns {@link StaffTestMob}s for {@code /testmob spawn}. */
@Singleton
public class StaffTestMobFactory extends SceneObjectFactory {

    @Inject
    public StaffTestMobFactory(@NotNull SceneObjectRegistry registry) {
        super("testmob", registry);
    }

    @Override
    public String[] getTypes() {
        return new String[]{"skeleton_warrior"};
    }

    @Override
    public StaffTestMob spawnDefault(@NotNull Location location, @NotNull String type) {
        return spawn(location);
    }

    /** Spawns a test mob standing at {@code location}, with its home there. */
    public StaffTestMob spawn(@NotNull Location location) {
        // Without randomized data the body never spawns as a baby or with gear.
        final Husk body = location.getWorld().spawn(location, Husk.class, false, spawned -> spawned.setPersistent(false));
        return spawn(new StaffTestMob(this), body);
    }
}
