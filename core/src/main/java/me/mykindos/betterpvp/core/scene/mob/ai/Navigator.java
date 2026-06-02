package me.mykindos.betterpvp.core.scene.mob.ai;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

/**
 * Thin wrapper over the backing entity's {@link org.bukkit.entity.Pathfinder} so AI components
 * don't each have to null-check and cast the entity. All methods no-op gracefully if the mob's
 * backing entity is not a {@link Mob} (e.g. an unusual placeholder), keeping components simple.
 */
public class Navigator {

    private final SceneMob mob;

    public Navigator(SceneMob mob) {
        this.mob = mob;
    }

    public void moveTo(Location location, double speed) {
        final Mob bukkitMob = mob.getBukkitMob();
        if (bukkitMob != null) {
            bukkitMob.getPathfinder().moveTo(location, speed);
        }
    }

    public void moveTo(LivingEntity target, double speed) {
        moveTo(target.getLocation(), speed);
    }

    public void stop() {
        final Mob bukkitMob = mob.getBukkitMob();
        if (bukkitMob != null) {
            bukkitMob.getPathfinder().stopPathfinding();
        }
    }

    public boolean isNavigating() {
        final Mob bukkitMob = mob.getBukkitMob();
        return bukkitMob != null && bukkitMob.getPathfinder().getCurrentPath() != null;
    }

}
