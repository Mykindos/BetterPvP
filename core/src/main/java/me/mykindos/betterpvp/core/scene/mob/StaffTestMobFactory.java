package me.mykindos.betterpvp.core.scene.mob;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Vindicator;
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
        return new String[0];
    }

    @Override
    public StaffTestMob spawnDefault(@NotNull Location location, @NotNull String type) {
        throw new UnsupportedOperationException("Test mobs are spawned with /testmob spawn");
    }

    /** Spawns a test mob standing at {@code location}, with its home there. */
    public StaffTestMob spawn(@NotNull Location location) {
        // Without randomized data the body never spawns with gear.
        final Vindicator body = location.getWorld().spawn(location, Vindicator.class, false, spawned -> {
            spawned.setPersistent(false);
            spawned.setRemoveWhenFarAway(false);
        });
        return spawn(new StaffTestMob(this), body);
    }
}
