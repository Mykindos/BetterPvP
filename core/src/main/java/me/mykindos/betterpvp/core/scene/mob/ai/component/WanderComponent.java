package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Range;

import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

/**
 * Idle ambient behaviour: when the mob has no target it periodically strolls to a random point
 * around its home anchor, returning to the IDLE clip whenever it arrives and pauses.
 * <p>
 * With {@link #rest} it walks each trip to the end and then rests for a random time. With {@link #checkFloor} it only
 * picks spots it can stand on.
 */
@Accessors(fluent = true, chain = true)
public class WanderComponent implements AIComponent {

    private final SceneMob mob;
    private final LongSupplier clock;

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

    private boolean resting;
    private long minRestMillis;
    private long maxRestMillis;
    private boolean floorChecked;
    private double minRadius;

    private long lastRepath = 0L;
    private boolean moving = false;
    private long restUntil = 0L;

    public WanderComponent(SceneMob mob) {
        this(mob, System::currentTimeMillis);
    }

    public WanderComponent(SceneMob mob, LongSupplier clock) {
        this.mob = mob;
        this.clock = clock;
    }

    /** Rests for a random time between {@code minMillis} and {@code maxMillis} after each trip, holding IDLE. */
    public WanderComponent rest(long minMillis, long maxMillis) {
        this.resting = true;
        this.minRestMillis = minMillis;
        this.maxRestMillis = maxMillis;
        return this;
    }

    /**
     * Picks spots at least {@code minRadius} from home where a solid block has two free blocks above it, instead of
     * any point at home's height. Liquid does not count as free.
     */
    public WanderComponent checkFloor(double minRadius) {
        this.floorChecked = true;
        this.minRadius = minRadius;
        return this;
    }

    @Override
    public EnumSet<AIControl> getControls() {
        return EnumSet.of(AIControl.MOVE);
    }

    @Override
    public boolean canStart() {
        return mob.getCurrentTarget() == null;
    }

    @Override
    public void tick() {
        if (resting) {
            tickResting();
            return;
        }

        if (mob.getNavigator().isNavigating()) {
            return;
        }

        // Arrived/idle: drop back to the idle clip once on the transition out of movement.
        if (moving) {
            moving = false;
            mob.stopMoving();
        }

        final long now = clock.getAsLong();
        if (now - lastRepath < repathCooldownMillis) {
            return;
        }
        lastRepath = now;

        pickPoint().ifPresent(point -> {
            mob.startMoving(point, wanderSpeed);
            moving = true;
        });
    }

    private void tickResting() {
        if (moving) {
            if (mob.getNavigator().hasArrived()) {
                rest();
            }
            return;
        }
        if (clock.getAsLong() < restUntil) {
            return;
        }
        final Optional<Location> point = pickPoint();
        if (point.isEmpty()) {
            rest();
            return;
        }
        mob.travelTo(point.get(), wanderSpeed, this::rest);
        moving = true;
    }

    private void rest() {
        moving = false;
        mob.stopMoving();
        restUntil = clock.getAsLong() + ThreadLocalRandom.current().nextLong(minRestMillis, maxRestMillis + 1);
    }

    private Optional<Location> pickPoint() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final Location home = mob.getHomeAnchor();
        if (!floorChecked) {
            final double angle = random.nextDouble(Math.PI * 2.0);
            final double distance = random.nextDouble(wanderRadius);
            return Optional.of(new Location(
                    home.getWorld(),
                    home.getX() + Math.cos(angle) * distance,
                    home.getY(),
                    home.getZ() + Math.sin(angle) * distance));
        }

        // Scanned upward so the lowest floor wins, never a ceiling above it.
        for (int attempt = 0; attempt < 8; attempt++) {
            final double angle = random.nextDouble(Math.PI * 2.0);
            final double distance = minRadius + random.nextDouble() * Math.max(0, wanderRadius - minRadius);
            final int x = home.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            final int z = home.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            for (int y = home.getBlockY() - 6; y <= home.getBlockY() + 3; y++) {
                final Block floor = home.getWorld().getBlockAt(x, y, z);
                if (floor.isSolid() && isOpen(floor.getRelative(0, 1, 0)) && isOpen(floor.getRelative(0, 2, 0))) {
                    return Optional.of(new Location(home.getWorld(), x + 0.5, y + 1, z + 0.5));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isOpen(Block block) {
        return block.isPassable() && !block.isLiquid();
    }

    @Override
    public void stop() {
        moving = false;
        restUntil = 0L;
        mob.stopMoving();
    }

}
