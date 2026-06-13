package me.mykindos.betterpvp.core.scene.behavior;

import me.mykindos.betterpvp.core.scene.npc.NPC;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

/**
 * Makes an NPC path toward a player each tick — a simple companion / "follow me"
 * behaviour. Requires the backing entity to be an AI-enabled {@link Mob} (so it
 * has a pathfinder). Re-paths on a short cadence to avoid spamming the navigator.
 */
public class FollowPlayerBehavior implements SceneBehavior {

    private static final int REPATH_TICKS = 10;

    private final NPC npc;
    private final Player target;
    private final double speed;
    private int counter = 0;

    public FollowPlayerBehavior(NPC npc, Player target, double speed) {
        this.npc = npc;
        this.target = target;
        this.speed = speed;
    }

    @Override
    public void tick() {
        if (!target.isOnline()) return;
        if (counter++ % REPATH_TICKS != 0) return;
        if (npc.isInitialized() && npc.getEntity() instanceof Mob mob) {
            mob.getPathfinder().moveTo(target.getLocation(), speed);
        }
    }

    @Override
    public void stop() {
        if (npc.isInitialized() && npc.getEntity() instanceof Mob mob) {
            mob.getPathfinder().stopPathfinding();
        }
    }
}
