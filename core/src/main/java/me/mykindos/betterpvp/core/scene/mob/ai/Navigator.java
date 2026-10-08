package me.mykindos.betterpvp.core.scene.mob.ai;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.Nullable;

/**
 * Thin wrapper over the backing entity's {@link org.bukkit.entity.Pathfinder} so AI components
 * don't each have to null-check and cast the entity. All methods no-op gracefully if the mob's
 * backing entity is not a {@link Mob} (e.g. an unusual placeholder), keeping components simple.
 * <p>
 * A trip started with {@link #travelTo} searches again when it has no path or the body stops moving, and gives up
 * after too many searches. Any other move or a stop ends the trip.
 */
public class Navigator {

    private static final double ARRIVAL_RADIUS = 1.5;
    private static final double ARRIVAL_HEIGHT = 2.5;
    private static final int REPATH_TICKS = 10;
    private static final int STUCK_TICKS = 100;
    private static final double STUCK_DISTANCE = 0.05;
    private static final int MAX_SEARCHES = 20;

    private final SceneMob mob;

    @Nullable private Location destination;
    private double speed;
    @Nullable private Runnable onGiveUp;
    private int searches;
    private int repathCooldown;
    private int stuckTicks;
    @Nullable private Location stuckFrom;

    public Navigator(SceneMob mob) {
        this.mob = mob;
    }

    public void moveTo(Location location, double speed) {
        destination = null;
        path(location, speed);
    }

    public void moveTo(LivingEntity target, double speed) {
        moveTo(target.getLocation(), speed);
    }

    /**
     * Starts a trip to a fixed point that searches again when it has no path or the body stops moving, and gives up
     * after too many searches. {@link #tick()} drives it.
     *
     * @param onGiveUp run once if the trip gives up
     */
    public void travelTo(Location location, double speed, Runnable onGiveUp) {
        this.destination = location.clone();
        this.speed = speed;
        this.onGiveUp = onGiveUp;
        this.searches = 0;
        search();
    }

    /** @return whether the body is at the point of the current trip */
    public boolean hasArrived() {
        final Mob bukkitMob = mob.getBukkitMob();
        if (destination == null || bukkitMob == null) {
            return false;
        }
        final Location current = bukkitMob.getLocation();
        if (!destination.getWorld().equals(current.getWorld())) {
            return false;
        }
        final double dx = current.getX() - destination.getX();
        final double dz = current.getZ() - destination.getZ();
        return dx * dx + dz * dz <= ARRIVAL_RADIUS * ARRIVAL_RADIUS
                && Math.abs(current.getY() - destination.getY()) <= ARRIVAL_HEIGHT;
    }

    /** Advances the current trip. */
    public void tick() {
        final Mob bukkitMob = mob.getBukkitMob();
        if (destination == null || bukkitMob == null || hasArrived()) {
            return;
        }

        if (repathCooldown > 0) {
            repathCooldown--;
        } else if (bukkitMob.getPathfinder().getCurrentPath() == null) {
            search();
            return;
        }

        final Location current = bukkitMob.getLocation();
        if (stuckFrom == null || !current.getWorld().equals(stuckFrom.getWorld())
                || current.distanceSquared(stuckFrom) > STUCK_DISTANCE * STUCK_DISTANCE) {
            stuckFrom = current;
            stuckTicks = 0;
        } else if (++stuckTicks >= STUCK_TICKS) {
            search();
        }
    }

    private void search() {
        if (destination == null) {
            return;
        }
        if (searches++ >= MAX_SEARCHES) {
            final Runnable giveUp = onGiveUp;
            destination = null;
            onGiveUp = null;
            if (giveUp != null) {
                giveUp.run();
            }
            return;
        }
        repathCooldown = REPATH_TICKS;
        stuckTicks = 0;
        stuckFrom = path(destination, speed);
    }

    /** @return where the body stood when the path was asked for, or {@code null} without a pathfinding body */
    @Nullable
    private Location path(Location location, double speed) {
        final Mob bukkitMob = mob.getBukkitMob();
        if (bukkitMob == null) {
            return null;
        }
        bukkitMob.getPathfinder().moveTo(location, speed);
        return bukkitMob.getLocation();
    }

    public void stop() {
        destination = null;
        onGiveUp = null;
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
