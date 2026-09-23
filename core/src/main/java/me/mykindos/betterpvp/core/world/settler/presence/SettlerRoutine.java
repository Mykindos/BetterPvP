package me.mykindos.betterpvp.core.world.settler.presence;

import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import me.mykindos.betterpvp.core.scene.behavior.SceneBehavior;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * What a settler does with its day: walks to its workplace and works there while it has one, and otherwise wanders
 * around its home, stopping for a while at each spot. It only moves while a player is close enough to see it.
 * <p>
 * Where it works is asked for each time it decides where to go, so a new assignment only needs {@link #replan()}.
 */
public class SettlerRoutine implements SceneBehavior {

    private static final double ARRIVAL_RADIUS = 1.5;
    private static final double ARRIVAL_HEIGHT = 2.5;
    private static final int STUCK_TICKS = 100;
    private static final int MAX_PATHS = 20;
    private static final int REPATH_TICKS = 10;
    private static final int PROXIMITY_TICKS = 20;
    private static final double ACTIVATION_RADIUS = 48;
    private static final double FOLLOW_RANGE = 48;
    private static final double SPEED = 0.6;
    private static final double WANDER_RADIUS = 12;
    private static final long MIN_REST_MILLIS = 5_000;
    private static final long MAX_REST_MILLIS = 15_000;

    private enum Phase { MOVING, RESTING, WORKING }

    private final SettlerNPC npc;
    private final SettlerLook look;
    private final Location home;
    private final Supplier<Optional<Location>> workplace;

    private Phase phase = Phase.RESTING;
    private long restUntil;
    private @Nullable Location target;
    private boolean toWork;
    private int paths;
    private int repathCooldown;
    private int stuckTicks;
    private @Nullable Location lastPosition;
    private int proximityCounter;
    private boolean watched = true;
    private boolean enabledAi;

    public SettlerRoutine(@NotNull SettlerNPC npc, @NotNull SettlerLook look, @NotNull Location home,
                          @NotNull Supplier<Optional<Location>> workplace) {
        this.npc = npc;
        this.look = look;
        this.home = home.clone();
        this.workplace = workplace;
    }

    @Override
    public void start() {
        final Mob mob = mob();
        if (mob == null) {
            return;
        }
        if (!mob.hasAI()) {
            mob.setAI(true);
            enabledAi = true;
        }
        Bukkit.getMobGoals().removeAllGoals(mob);
        final AttributeInstance followRange = mob.getAttribute(Attribute.FOLLOW_RANGE);
        if (followRange != null && followRange.getBaseValue() < FOLLOW_RANGE) {
            followRange.setBaseValue(FOLLOW_RANGE);
        }
        animate(false);
        replan();
    }

    @Override
    public void stop() {
        final Mob mob = mob();
        if (mob != null) {
            mob.getPathfinder().stopPathfinding();
            if (enabledAi) {
                mob.setAI(false);
                enabledAi = false;
            }
        }
    }

    /** Decides afresh where to go, such as after its assignment changed. */
    public void replan() {
        final Mob mob = mob();
        if (mob == null) {
            return;
        }
        final Optional<Location> work = workplace.get();
        toWork = work.isPresent();
        target = work.orElseGet(() -> wanderPoint().orElse(null));
        if (target == null) {
            rest();
            return;
        }
        animate(false);
        phase = Phase.MOVING;
        paths = 0;
        stuckTicks = 0;
        lastPosition = mob.getLocation();
        path(mob);
    }

    @Override
    public void tick() {
        final Mob mob = mob();
        if (mob == null) {
            return;
        }
        if (proximityCounter-- <= 0) {
            proximityCounter = PROXIMITY_TICKS;
            watched = !mob.getWorld().getNearbyPlayers(mob.getLocation(), ACTIVATION_RADIUS).isEmpty();
            if (!watched) {
                mob.getPathfinder().stopPathfinding();
            }
        }
        if (!watched) {
            return;
        }

        switch (phase) {
            case RESTING -> {
                if (System.currentTimeMillis() >= restUntil) {
                    replan();
                }
            }
            case MOVING -> move(mob);
            case WORKING -> {
            }
        }
    }

    private void move(@NotNull Mob mob) {
        if (target == null) {
            rest();
            return;
        }
        if (arrived(mob, target)) {
            mob.getPathfinder().stopPathfinding();
            if (toWork) {
                phase = Phase.WORKING;
                animate(true);
            } else {
                rest();
            }
            return;
        }

        if (repathCooldown > 0) {
            repathCooldown--;
        } else if (mob.getPathfinder().getCurrentPath() == null && !path(mob)) {
            rest();
            return;
        }

        final Location current = mob.getLocation();
        if (lastPosition != null && current.distanceSquared(lastPosition) > 0.0025) {
            stuckTicks = 0;
        } else if (++stuckTicks >= STUCK_TICKS) {
            stuckTicks = 0;
            if (!path(mob)) {
                rest();
                return;
            }
        }
        lastPosition = current;
    }

    /** @return false once this leg has used up its searches, so an unreachable spot is given up on */
    private boolean path(@NotNull Mob mob) {
        if (target == null || paths++ >= MAX_PATHS) {
            return false;
        }
        repathCooldown = REPATH_TICKS;
        mob.getPathfinder().moveTo(target, SPEED);
        return true;
    }

    private void rest() {
        phase = Phase.RESTING;
        target = null;
        animate(false);
        restUntil = System.currentTimeMillis()
                + ThreadLocalRandom.current().nextLong(MIN_REST_MILLIS, MAX_REST_MILLIS);
    }

    /** A spot near home with room to stand, searched a little above and below home so it never picks a roof. */
    private @NotNull Optional<Location> wanderPoint() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 8; attempt++) {
            final double angle = random.nextDouble(Math.PI * 2);
            final double distance = random.nextDouble(2, WANDER_RADIUS);
            final int x = home.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            final int z = home.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            for (int y = home.getBlockY() + 3; y >= home.getBlockY() - 6; y--) {
                final Block floor = home.getWorld().getBlockAt(x, y, z);
                if (floor.getType().isSolid() && floor.getRelative(0, 1, 0).isPassable()
                        && floor.getRelative(0, 2, 0).isPassable()) {
                    return Optional.of(new Location(home.getWorld(), x + 0.5, y + 1, z + 0.5));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean arrived(@NotNull Mob mob, @NotNull Location target) {
        final Location current = mob.getLocation();
        if (!current.getWorld().equals(target.getWorld())) {
            return false;
        }
        final double dx = current.getX() - target.getX();
        final double dz = current.getZ() - target.getZ();
        return dx * dx + dz * dz <= ARRIVAL_RADIUS * ARRIVAL_RADIUS
                && Math.abs(current.getY() - target.getY()) <= ARRIVAL_HEIGHT;
    }

    /** Standing plays the work animation at the workplace and the idle one anywhere else. */
    private void animate(boolean working) {
        final ModeledEntity modeled = npc.getModeledEntity();
        if (modeled == null) {
            return;
        }
        for (ActiveModel model : modeled.getModels().values()) {
            final AnimationHandler handler = model.getAnimationHandler();
            handler.setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.WALK, look.getWalkAnimation(), 0.2, 0.2, 1));
            handler.setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.IDLE,
                    working ? look.getWorkAnimation() : look.getIdleAnimation(), 0.2, 0.2, 1));
        }
    }

    private @Nullable Mob mob() {
        return npc.isMaterialized() && npc.getEntity() instanceof Mob mob ? mob : null;
    }
}
