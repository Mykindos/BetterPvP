package me.mykindos.betterpvp.core.scene.behavior;

import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.entity.Mob;

/**
 * Walks the straight segment between consecutive waypoints, so the NPC traces the authored route.
 * <p>
 * This drives the entity's move controller directly instead of asking the pathfinder for a route,
 * which is what makes the line predictable: there is no search, so there is no range limit, no
 * partial path, and no opinion of its own about where the NPC ought to walk. Step-ups and the turn
 * toward the target are still handled by the controller, so the NPC climbs a kerb or a stair as
 * normal.
 * <p>
 * The trade is that it will not route around anything. A resident that repeatedly skips the same leg
 * is reporting that the straight line between those two waypoints is blocked - the fix is another
 * waypoint at the corner, not a longer timeout. Choose {@link PathfindNavigator} when the route runs
 * through terrain the author cannot fully anticipate.
 */
public class DirectNavigator implements PatrolNavigator {

    @Override
    public void begin(Mob mob, Location target, double speed) {
        walkToward(mob, target, speed);
    }

    @Override
    public boolean advance(Mob mob, Location target, double speed) {
        // Re-asserted every tick: the controller drops back to waiting once it believes it has
        // arrived, and refreshing the target is what carries the NPC through the whole segment.
        walkToward(mob, target, speed);
        return true;
    }

    @Override
    public boolean recover(Mob mob, Location target, double speed) {
        // Nothing to retry - the next attempt would walk the same blocked line into the same wall.
        return false;
    }

    @Override
    public void stop(Mob mob) {
        // The move controller has no cancel; asking it to stand where it already is settles it.
        final Location standing = mob.getLocation();
        walkToward(mob, standing, 0);
    }

    private void walkToward(Mob mob, Location target, double speed) {
        ((CraftMob) mob).getHandle().getMoveControl()
                .setWantedPosition(target.getX(), target.getY(), target.getZ(), speed);
    }

}
