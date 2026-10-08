package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

/**
 * Sends the mob to a random point 1 to 3 blocks from a spot after {@link #orderTo(Location)}, rests there, then lets
 * the components below it decide again. An order interrupted by a higher component carries on afterwards.
 */
public class OrderToSpotComponent implements AIComponent {

    private final SceneMob mob;
    private final LongSupplier clock;

    private double speed = 1.0;
    private long minRestMillis = 5_000L;
    private long maxRestMillis = 15_000L;

    @Nullable private Location destination;
    private boolean running;
    private boolean resting;
    private long restUntil;

    public OrderToSpotComponent(SceneMob mob) {
        this(mob, System::currentTimeMillis);
    }

    public OrderToSpotComponent(SceneMob mob, LongSupplier clock) {
        this.mob = mob;
        this.clock = clock;
    }

    /** Pathfinding speed multiplier used on the way to the spot. */
    public OrderToSpotComponent speed(double speed) {
        this.speed = speed;
        return this;
    }

    /** How long the mob rests at the spot, between {@code minMillis} and {@code maxMillis}. */
    public OrderToSpotComponent rest(long minMillis, long maxMillis) {
        this.minRestMillis = minMillis;
        this.maxRestMillis = maxMillis;
        return this;
    }

    /** Sends the mob to a point near {@code spot}. */
    public void orderTo(Location spot) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final double angle = random.nextDouble(Math.PI * 2);
        final double distance = random.nextDouble(1, 3);
        destination = spot.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
        resting = false;
        if (running) {
            start();
        }
    }

    @Override
    public EnumSet<AIControl> getControls() {
        return EnumSet.of(AIControl.MOVE);
    }

    @Override
    public boolean canStart() {
        return destination != null && !(resting && clock.getAsLong() >= restUntil);
    }

    @Override
    public void start() {
        running = true;
        if (resting) {
            mob.stopMoving();
        } else {
            mob.travelTo(destination, speed, this::rest);
        }
    }

    @Override
    public void tick() {
        if (!resting && mob.getNavigator().hasArrived()) {
            rest();
        }
    }

    private void rest() {
        resting = true;
        mob.stopMoving();
        restUntil = clock.getAsLong() + ThreadLocalRandom.current().nextLong(minRestMillis, maxRestMillis + 1);
    }

    @Override
    public void stop() {
        running = false;
        if (resting && clock.getAsLong() >= restUntil) {
            destination = null;
            resting = false;
        }
        mob.stopMoving();
    }

}
