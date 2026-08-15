package me.mykindos.betterpvp.core.scene.behavior;

import com.ticxo.modelengine.api.animation.ModelState;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.mykindos.betterpvp.core.scene.HasModeledEntity;
import me.mykindos.betterpvp.core.scene.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Walks an NPC along an ordered route, pausing at waypoints that ask for it and cycling according to
 * the configured {@link PatrolMode}.
 * <p>
 * How it travels between two waypoints is the {@link PatrolNavigator}'s decision: straight down the
 * authored line by default, or via the server's pathfinder if the route needs to cope with terrain
 * the author could not anticipate.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * npc.addBehavior(new WaypointPatrolBehavior(npc, waypoints, PatrolMode.CIRCULAR, 1.0, "walk", "idle"));
 * }</pre>
 *
 * <h3>Attach in onInit, not the constructor</h3>
 * For a chunk-managed NPC this must be attached from {@code onInit()} (or a decorator), because
 * {@code SceneEntity#onDematerialize} clears behaviours on every chunk unload and {@code onInit} is
 * what re-adds them. A behaviour attached in the constructor exists only until the first chunk cycle.
 *
 * <h3>Movement requirements</h3>
 * The backing entity must be a {@link Mob}. Scene backing entities are normally spawned with AI off,
 * which silently disables all locomotion, so this behaviour enables AI and strips the vanilla goals
 * itself on {@link #start()} - otherwise those goals would fight it every tick - and restores the
 * entity on {@link #stop()}. Callers therefore do not need a special entity factory.
 *
 * <h3>Animation</h3>
 * Walk/idle clips are optional and are registered as ModelEngine {@link ModelState} clips, so the
 * engine drives them from the entity's own movement and only ever runs one at a time. They apply to
 * any {@link HasModeledEntity} NPC and no-op when no model is bound.
 */
public class WaypointPatrolBehavior implements SceneBehavior {

    /**
     * Horizontal distance (blocks) within which the NPC counts as having arrived. Cannot usefully be
     * tightened under {@link PathfindNavigator}: it aims at the target's <i>block centre</i> and ends
     * the path within about half a block of that, so an off-centre marker leaves the NPC legitimately
     * parked over a block away. A smaller radius is then never satisfied and every leg ends in the
     * stall detector instead. It also rounds corners slightly, which reads better than stopping dead.
     */
    private static final double ARRIVAL_RADIUS = 1.5;

    /**
     * Vertical slack (blocks) on arrival. Generous on purpose: a marker placed at eye height, or an
     * NPC standing on a slab, must still satisfy arrival or the route stalls forever on that leg.
     */
    private static final double ARRIVAL_HEIGHT = 2.5;

    /** Ticks of no measurable movement before a leg is treated as stuck. */
    private static final int STUCK_TICKS = 100;

    /** Squared distance below which a tick counts as "no progress". */
    private static final double PROGRESS_EPSILON_SQ = 0.0025;

    /** How often the activation-radius check is sampled, in ticks. */
    private static final int PROXIMITY_CHECK_TICKS = 20;

    private enum Phase { MOVING, DWELLING }

    private final NPC npc;
    private final List<Waypoint> waypoints;
    private final PatrolMode mode;
    private final double speed;
    @Nullable private final String walkAnimation;
    @Nullable private final String idleAnimation;

    /** Patrol only runs while a player is within this many blocks; 0 disables the gate. */
    @Setter
    @Accessors(fluent = true, chain = true)
    private double activationRadius = 48.0;

    /**
     * How the NPC travels between waypoints. Direct by default, because a route is authored as the
     * line the NPC should walk and only {@link DirectNavigator} actually walks it - the pathfinder
     * treats the same waypoints as anchors and picks its own way between them. Must be set before the
     * behaviour is attached, since {@link #start()} prepares the backing entity for it.
     */
    @Setter
    @Accessors(fluent = true, chain = true)
    private PatrolNavigator navigator = new DirectNavigator();

    private int index = 0;
    /** +1 = forward, -1 = backward. Only meaningful for {@link PatrolMode#BACKTRACK}. */
    private int direction = 1;

    private Phase phase = Phase.MOVING;
    private long dwellUntil = 0L;
    private boolean walking = false;

    private int stuckTicks = 0;
    @Nullable private Location lastPosition;

    private int proximityCounter = 0;
    private boolean withinRange = true;

    /** Whether this behaviour turned the entity's AI on, so {@link #stop()} only undoes its own change. */
    private boolean enabledAi = false;

    /**
     * @param npc           The NPC to move. Its entity must be a {@link Mob} for movement to work.
     * @param waypoints     Ordered route. Must contain at least 2 entries.
     * @param mode          How to cycle after reaching the last waypoint.
     * @param speed         Movement speed multiplier (1.0 = normal walk speed).
     * @param walkAnimation ModelEngine animation ID to play while walking, or {@code null}.
     * @param idleAnimation ModelEngine animation ID to play while standing, or {@code null}.
     */
    public WaypointPatrolBehavior(NPC npc, List<Waypoint> waypoints, PatrolMode mode, double speed,
                                  @Nullable String walkAnimation, @Nullable String idleAnimation) {
        if (waypoints.size() < 2) throw new IllegalArgumentException("Patrol requires at least 2 waypoints");
        this.npc = npc;
        this.waypoints = List.copyOf(waypoints);
        this.mode = mode;
        this.speed = speed;
        this.walkAnimation = walkAnimation;
        this.idleAnimation = idleAnimation;
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
        // Vanilla goals would re-target the mob every tick and undo our movement.
        Bukkit.getMobGoals().removeAllGoals(mob);
        navigator.prepare(mob, waypoints);
        registerStateAnimations();
        beginLeg(mob);
    }

    @Override
    public void stop() {
        final Mob mob = mob();
        if (mob != null) {
            navigator.stop(mob);
            if (enabledAi) {
                mob.setAI(false);
                enabledAi = false;
            }
        }
        walking = false;
    }

    @Override
    public void tick() {
        final Mob mob = mob();
        if (mob == null) {
            return;
        }

        if (!withinActivationRange(mob)) {
            pause(mob);
            return;
        }

        switch (phase) {
            case DWELLING -> {
                if (System.currentTimeMillis() < dwellUntil) {
                    return;
                }
                advanceWaypoint();
                beginLeg(mob);
            }
            case MOVING -> {
                if (hasArrived(mob, waypoints.get(index))) {
                    arrive(mob);
                    return;
                }
                if (!navigator.advance(mob, waypoints.get(index).getLocation(), speed)) {
                    abandonLeg(mob);
                    return;
                }
                checkProgress(mob);
            }
        }
    }

    /** Starts walking toward the current waypoint and resets the stuck detector for that leg. */
    private void beginLeg(Mob mob) {
        phase = Phase.MOVING;
        stuckTicks = 0;
        lastPosition = mob.getLocation();
        navigator.begin(mob, waypoints.get(index).getLocation(), speed);
        walking = true;
    }

    /**
     * Gives up on a waypoint the navigator cannot reach and moves on to the next one. A waypoint
     * behind a locked door would otherwise hold the entire route for the rest of the server's life.
     */
    private void abandonLeg(Mob mob) {
        advanceWaypoint();
        beginLeg(mob);
    }

    /** Handles reaching a waypoint: stop, adopt its facing, and either dwell or move straight on. */
    private void arrive(Mob mob) {
        final Waypoint waypoint = waypoints.get(index);
        navigator.stop(mob);

        if (waypoint.getDwellMillis() <= 0) {
            advanceWaypoint();
            beginLeg(mob);
            return;
        }

        walking = false;
        final Location facing = mob.getLocation();
        facing.setYaw(waypoint.getLocation().getYaw());
        facing.setPitch(waypoint.getLocation().getPitch());
        mob.teleport(facing);

        phase = Phase.DWELLING;
        dwellUntil = System.currentTimeMillis() + waypoint.getDwellMillis();
    }

    /**
     * Detects an NPC that is being told to move but is not moving - wedged on geometry, or walking
     * into a wall - and hands it to the navigator to recover or write off.
     */
    private void checkProgress(Mob mob) {
        final Location current = mob.getLocation();
        if (lastPosition != null && current.getWorld().equals(lastPosition.getWorld())
                && current.distanceSquared(lastPosition) > PROGRESS_EPSILON_SQ) {
            stuckTicks = 0;
        } else {
            stuckTicks++;
        }
        lastPosition = current;

        if (stuckTicks < STUCK_TICKS) {
            return;
        }
        stuckTicks = 0;
        if (!navigator.recover(mob, waypoints.get(index).getLocation(), speed)) {
            abandonLeg(mob);
        }
    }

    /** Halts movement without losing route position, for when no player is watching. */
    private void pause(Mob mob) {
        if (walking) {
            walking = false;
            navigator.stop(mob);
        }
    }

    /**
     * @return whether a player is close enough for the patrol to be worth running. Sampled rather than
     * tested every tick, since pathfinding is the expensive part and nobody can see the difference.
     */
    private boolean withinActivationRange(Mob mob) {
        if (activationRadius <= 0) {
            return true;
        }
        if (proximityCounter-- <= 0) {
            proximityCounter = PROXIMITY_CHECK_TICKS;
            withinRange = !mob.getWorld().getNearbyPlayers(mob.getLocation(), activationRadius).isEmpty();
        }
        return withinRange;
    }

    /**
     * @return whether the NPC is close enough to {@code waypoint} to count as arrived. Horizontal and
     * vertical distance are tested separately so a marker's height does not have to be exact.
     */
    private boolean hasArrived(Mob mob, Waypoint waypoint) {
        final Location current = mob.getLocation();
        final Location target = waypoint.getLocation();
        if (target.getWorld() != null && !current.getWorld().equals(target.getWorld())) {
            return false;
        }
        final double dx = current.getX() - target.getX();
        final double dz = current.getZ() - target.getZ();
        if (dx * dx + dz * dz > ARRIVAL_RADIUS * ARRIVAL_RADIUS) {
            return false;
        }
        return Math.abs(current.getY() - target.getY()) <= ARRIVAL_HEIGHT;
    }

    private void advanceWaypoint() {
        switch (mode) {
            case CIRCULAR -> index = (index + 1) % waypoints.size();
            case BACKTRACK -> {
                index += direction;
                if (index >= waypoints.size() - 1) {
                    index = waypoints.size() - 1;
                    direction = -1;
                } else if (index <= 0) {
                    index = 0;
                    direction = 1;
                }
            }
        }
    }

    @Nullable
    private Mob mob() {
        if (!npc.isInitialized() || !(npc.getEntity() instanceof Mob mob) || !mob.isValid()) {
            return null;
        }
        return mob;
    }

    /**
     * Registers walk and idle as ModelEngine <i>state</i> clips instead of playing them by hand.
     * State clips live in the priority handler's lowest tier and are mutually exclusive - the engine
     * picks one per tick from the entity's real movement - so idle takes over the moment the NPC stops
     * and neither clip has to be stopped explicitly. Played by hand, walk would sit in a higher tier
     * and mask the idle state for as long as it looped. No-op without a bound model.
     */
    private void registerStateAnimations() {
        if (!(npc instanceof HasModeledEntity modeled)) {
            return;
        }
        final ModeledEntity modeledEntity = modeled.getModeledEntity();
        if (modeledEntity == null) {
            return;
        }
        for (ActiveModel model : modeledEntity.getModels().values()) {
            final AnimationHandler handler = model.getAnimationHandler();
            if (walkAnimation != null) {
                handler.setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.WALK, walkAnimation, 0.2, 0.2, 1));
            }
            if (idleAnimation != null) {
                handler.setDefaultProperty(new AnimationHandler.DefaultProperty(ModelState.IDLE, idleAnimation, 0.2, 0.2, 1));
            }
        }
    }

}
