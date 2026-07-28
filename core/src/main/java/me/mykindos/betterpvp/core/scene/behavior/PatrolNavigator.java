package me.mykindos.betterpvp.core.scene.behavior;

import org.bukkit.Location;
import org.bukkit.entity.Mob;

import java.util.List;

/**
 * How a patrolling NPC actually travels from one waypoint to the next.
 * <p>
 * The two implementations promise different things, and the choice is a content decision rather than
 * a technical one. {@link PathfindNavigator} asks the server for a route and so copes with geometry
 * the route's author never anticipated, but it picks its own line between the waypoints and therefore
 * does not trace the authored path. {@link DirectNavigator} walks the straight segment between
 * consecutive waypoints, so the NPC follows exactly what was drawn, at the cost of needing a waypoint
 * wherever the route bends around something.
 *
 * @see WaypointPatrolBehavior#navigator(PatrolNavigator)
 */
public interface PatrolNavigator {

    /** One-time preparation of the backing entity when the patrol starts. */
    default void prepare(Mob mob, List<Waypoint> route) {
    }

    /** Begins a fresh leg toward {@code target}. */
    void begin(Mob mob, Location target, double speed);

    /**
     * Drives one tick of a leg already in progress.
     *
     * @return {@code false} if the leg cannot be completed and the waypoint should be given up
     */
    boolean advance(Mob mob, Location target, double speed);

    /**
     * Called once the NPC has spent a sustained period without moving, to attempt a recovery.
     *
     * @return {@code false} if the leg cannot be completed and the waypoint should be given up
     */
    boolean recover(Mob mob, Location target, double speed);

    /** Halts movement. The patrol keeps its place on the route, so a later leg can resume. */
    void stop(Mob mob);

}
