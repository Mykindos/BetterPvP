package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Location;
import org.jetbrains.annotations.Range;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Idle ambient behaviour: when the mob has no target it periodically strolls to a random point
 * around its home anchor, returning to the IDLE clip whenever it arrives and pauses.
 */
@Accessors(fluent = true, chain = true)
public class WanderComponent implements AIComponent {

    private final SceneMob mob;

    /** Maximum distance (in blocks) from home a wander point may be picked. */
    @Setter
    @Range(from = 0, to = Long.MAX_VALUE)
    private double wanderRadius = 8.0;
    /** Minimum delay between picking new wander destinations, in milliseconds. */
    @Setter
    @Range(from = 0, to = Long.MAX_VALUE)
    private long repathCooldownMillis = 3000L;
    /** Pathfinding speed multiplier used while strolling. */
    @Setter
    @Range(from = 0, to = Long.MAX_VALUE)
    private double wanderSpeed = 0.8;

    private long lastRepath = 0L;
    private boolean moving = false;

    public WanderComponent(SceneMob mob) {
        this.mob = mob;
    }

    @Override
    public EnumSet<AIControl> getControls() {
        return EnumSet.of(AIControl.MOVE, AIControl.LOOK);
    }

    @Override
    public boolean canStart() {
        return mob.getCurrentTarget() == null;
    }

    @Override
    public void tick() {
        if (mob.getNavigator().isNavigating()) {
            return;
        }

        // Arrived/idle: drop back to the idle clip once on the transition out of movement.
        if (moving) {
            moving = false;
            mob.stopMoving();
        }

        final long now = System.currentTimeMillis();
        if (now - lastRepath < repathCooldownMillis) {
            return;
        }
        lastRepath = now;

        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final double angle = random.nextDouble(Math.PI * 2.0);
        final double distance = random.nextDouble(wanderRadius);
        final Location home = mob.getHomeAnchor();
        final Location point = new Location(
                home.getWorld(),
                home.getX() + Math.cos(angle) * distance,
                home.getY(),
                home.getZ() + Math.sin(angle) * distance);

        mob.startMoving(point, wanderSpeed);
        moving = true;
    }

    @Override
    public void stop() {
        moving = false;
        mob.stopMoving();
    }

}
