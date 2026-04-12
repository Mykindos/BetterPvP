package me.mykindos.betterpvp.core.scene.behavior;

import com.ticxo.modelengine.api.model.ActiveModel;
import me.mykindos.betterpvp.core.scene.npc.ModeledNPC;
import me.mykindos.betterpvp.core.scene.npc.NPC;
import me.mykindos.betterpvp.core.utilities.ModelEngineHelper;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Walks an NPC along an ordered list of waypoints using the server's native pathfinder.
 * <p>
 * The behaviour detects arrival by distance and then advances to the next waypoint according
 * to the configured {@link PatrolMode}. If the NPC is a {@link ModeledNPC}, a walk animation
 * is played when movement begins and an idle animation when the NPC arrives at a point
 * (or when the behaviour is stopped).
 *
 * <h3>Example - circular patrol with animations</h3>
 * <pre>{@code
 * npc.addBehavior(new WaypointPatrolBehavior(
 *     npc,
 *     List.of(pointA, pointB, pointC),
 *     PatrolMode.CIRCULAR,
 *     1.0,          // speed multiplier (1.0 = normal walk)
 *     "walk",       // ModelEngine animation ID played while moving
 *     "idle"        // ModelEngine animation ID played while standing
 * ));
 * }</pre>
 *
 * <h3>Animation notes</h3>
 * Walk/idle animations are optional - pass {@code null} to skip animation changes entirely.
 * They are only triggered on <em>transitions</em> (idle→walk, walk→idle), not every tick,
 * so ModelEngine does not restart the clip on every update.
 * <p>
 * <b>Note:</b> this behavior is NPC-only (requires pathfinding) and should not be added to props.
 */
public class WaypointPatrolBehavior implements SceneBehavior {

    /** Squared distance (blocks²) within which the NPC is considered to have arrived. */
    private static final double ARRIVAL_THRESHOLD_SQ = 1.5 * 1.5;

    private final NPC npc;
    private final List<Location> waypoints;
    private final PatrolMode mode;
    private final double speed;
    @Nullable private final String walkAnimation;
    @Nullable private final String idleAnimation;

    private int index = 0;
    /** +1 = forward, -1 = backward. Only meaningful for {@link PatrolMode#BACKTRACK}. */
    private int direction = 1;

    /**
     * @param npc           The NPC to move. Its entity must be a {@link Mob} for pathfinding to work.
     * @param waypoints     Ordered patrol points. Must contain at least 2 entries.
     * @param mode          How to cycle after reaching the last waypoint.
     * @param speed         Pathfinder speed multiplier (1.0 = normal walk speed).
     * @param walkAnimation ModelEngine animation ID to play while walking, or {@code null}.
     * @param idleAnimation ModelEngine animation ID to play while standing, or {@code null}.
     */
    public WaypointPatrolBehavior(NPC npc, List<Location> waypoints, PatrolMode mode, double speed,
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
        moveTo(waypoints.get(index));
    }

    @Override
    public void stop() {
        if (npc.getEntity() instanceof Mob mob) {
            mob.getPathfinder().stopPathfinding();
        }
        playAnimation(idleAnimation);
    }

    @Override
    public void tick() {
        if (!(npc.getEntity() instanceof Mob)) return;
        if (!hasArrived(waypoints.get(index))) return;

        advanceWaypoint();
        moveTo(waypoints.get(index));
    }

    private boolean hasArrived(Location target) {
        final Location current = npc.getEntity().getLocation();
        if (!current.getWorld().equals(target.getWorld())) return false;
        return current.distanceSquared(target) <= ARRIVAL_THRESHOLD_SQ;
    }

    private void moveTo(Location target) {
        if (!(npc.getEntity() instanceof Mob mob)) return;
        mob.getPathfinder().moveTo(target, speed);
        playAnimation(walkAnimation);
    }

    private void advanceWaypoint() {
        switch (mode) {
            case CIRCULAR -> index = (index + 1) % waypoints.size();
            case BACKTRACK -> {
                index += direction;
                if (index >= waypoints.size() - 1) direction = -1;
                else if (index <= 0) direction = 1;
            }
        }
    }

    /**
     * Plays an animation on all {@link ActiveModel}s when the NPC is a {@link ModeledNPC}.
     * No-op if {@code animationId} is null or the NPC has no ModelEngine model.
     */
    private void playAnimation(@Nullable String animationId) {
        if (animationId == null || !(npc instanceof ModeledNPC modeledNPC)) return;
        for (ActiveModel model : modeledNPC.getModeledEntity().getModels().values()) {
            ModelEngineHelper.playAnimation(model, animationId);
        }
    }

}
