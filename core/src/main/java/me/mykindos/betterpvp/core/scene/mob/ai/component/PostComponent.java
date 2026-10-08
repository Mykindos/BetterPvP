package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import me.mykindos.betterpvp.core.scene.mob.animation.MobAnimation;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Walks the mob to its post and holds WORK there until it is replanned. The post is asked for only when the component
 * decides where to go: on its first start, after a rest, and after a replan or an interrupted trip. When the trip
 * gives up, the mob rests and then asks again. While there is no post it asks again after each rest.
 */
public class PostComponent implements AIComponent {

    private enum Phase { UNDECIDED, NO_POST, TRAVELLING, WORKING, RESTING }

    private final SceneMob mob;
    private final Supplier<Optional<Location>> post;
    private final LongSupplier clock;

    private double speed = 1.0;
    private long minRestMillis = 5_000L;
    private long maxRestMillis = 15_000L;

    @Nullable private Location destination;
    private Phase phase = Phase.UNDECIDED;
    private long restUntil;

    public PostComponent(SceneMob mob, Supplier<Optional<Location>> post) {
        this(mob, post, System::currentTimeMillis);
    }

    public PostComponent(SceneMob mob, Supplier<Optional<Location>> post, LongSupplier clock) {
        this.mob = mob;
        this.post = post;
        this.clock = clock;
    }

    /** Pathfinding speed multiplier used on the way to the post. */
    public PostComponent speed(double speed) {
        this.speed = speed;
        return this;
    }

    /**
     * How long the mob rests after a trip gives up, or before it asks again when it has no post, between
     * {@code minMillis} and {@code maxMillis}.
     */
    public PostComponent rest(long minMillis, long maxMillis) {
        this.minRestMillis = minMillis;
        this.maxRestMillis = maxMillis;
        return this;
    }

    @Override
    public EnumSet<AIControl> getControls() {
        return EnumSet.of(AIControl.MOVE);
    }

    @Override
    public boolean canStart() {
        if ((phase == Phase.RESTING || phase == Phase.NO_POST) && clock.getAsLong() >= restUntil) {
            phase = Phase.UNDECIDED;
        }
        if (phase == Phase.UNDECIDED) {
            destination = post.get().orElse(null);
            if (destination == null) {
                phase = Phase.NO_POST;
                restUntil = restEnd();
            } else {
                phase = Phase.TRAVELLING;
            }
        }
        return phase != Phase.NO_POST;
    }

    @Override
    public boolean shouldContinue() {
        return phase != Phase.RESTING || clock.getAsLong() < restUntil;
    }

    @Override
    public void start() {
        if (phase == Phase.RESTING) {
            mob.stopMoving();
            return;
        }
        phase = Phase.TRAVELLING;
        mob.travelTo(destination, speed, this::giveUp);
    }

    @Override
    public void tick() {
        if (phase == Phase.TRAVELLING && mob.getNavigator().hasArrived()) {
            phase = Phase.WORKING;
            mob.getNavigator().stop();
            mob.getAnimations().play(MobAnimation.WORK);
        }
    }

    private void giveUp() {
        phase = Phase.RESTING;
        mob.stopMoving();
        restUntil = restEnd();
    }

    private long restEnd() {
        return clock.getAsLong() + ThreadLocalRandom.current().nextLong(minRestMillis, maxRestMillis + 1);
    }

    @Override
    public void stop() {
        if (phase != Phase.RESTING) {
            phase = Phase.UNDECIDED;
        }
        mob.stopMoving();
    }

    @Override
    public void replan() {
        phase = Phase.UNDECIDED;
    }

}
