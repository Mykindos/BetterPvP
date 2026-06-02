package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

import java.util.EnumSet;

/**
 * Keeps an owned mob near its owner: it follows when the owner drifts beyond {@code followRange},
 * and teleports directly to the owner once they are further than {@code teleportRange} (e.g. the
 * owner walked through a portal). Yields entirely while the mob has a combat target.
 */
@Accessors(fluent = true, chain = true)
public class FollowOwnerComponent implements AIComponent {

    private final SceneMob mob;

    /** Distance (in blocks) the owner may drift before the mob starts pathing towards them. */
    @Setter
    @Range(from = 0, to = Long.MAX_VALUE)
    private double followRange = 5.0;
    /** Distance (in blocks) beyond which the mob teleports straight to the owner instead of walking. */
    @Setter
    @Range(from = 0, to = Long.MAX_VALUE)
    private double teleportRange = 25.0;
    /** Pathfinding speed multiplier used while following the owner. */
    @Setter
    @Range(from = 0, to = Long.MAX_VALUE)
    private double followSpeed = 1.0;

    public FollowOwnerComponent(SceneMob mob) {
        this.mob = mob;
    }

    @Override
    public EnumSet<AIControl> getControls() {
        return EnumSet.of(AIControl.MOVE);
    }

    @Override
    public boolean canStart() {
        if (mob.getCurrentTarget() != null) {
            return false; // yield entirely to combat while the mob has a target (per the contract above)
        }
        final Player owner = mob.getActiveOwner();
        if (owner == null) {
            return false;
        }
        return mob.getEntity().getLocation().distanceSquared(owner.getLocation()) > followRange * followRange;
    }

    @Override
    public void tick() {
        final Player owner = mob.getActiveOwner();
        if (owner == null) {
            return;
        }

        final double distanceSquared = mob.getEntity().getLocation().distanceSquared(owner.getLocation());
        if (distanceSquared > teleportRange * teleportRange) {
            mob.getEntity().teleport(owner.getLocation());
            return;
        }

        mob.startMoving(owner, followSpeed);
    }

    @Override
    public void stop() {
        mob.stopMoving();
    }

}
