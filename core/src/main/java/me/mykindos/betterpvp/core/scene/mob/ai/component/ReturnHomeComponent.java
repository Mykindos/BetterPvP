package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Location;
import org.jetbrains.annotations.Range;

import java.util.EnumSet;

/**
 * Leashes the mob to its home anchor: once it strays beyond {@code leashRange} it drops its
 * target and walks back home, continuing until it is comfortably close to the anchor again.
 */
@Accessors(fluent = true, chain = true)
public class ReturnHomeComponent implements AIComponent {

    private final SceneMob mob;

    /** Distance (in blocks) from home the mob may stray before it abandons combat and walks back. */
    @Setter
    @Range(from = 0, to = Long.MAX_VALUE)
    private double leashRange = 30.0;
    /** Fraction of {@code leashRange} the mob must get within before it counts as "home" again. */
    @Setter
    @Range(from = 0, to = 1)
    private double arrivalFactor = 0.5;
    /** Pathfinding speed multiplier used while walking home. */
    @Setter
    @Range(from = 0, to = Long.MAX_VALUE)
    private double returnSpeed = 1.0;

    public ReturnHomeComponent(SceneMob mob) {
        this.mob = mob;
    }

    @Override
    public EnumSet<AIControl> getControls() {
        return EnumSet.of(AIControl.MOVE);
    }

    @Override
    public boolean canStart() {
        final Location home = mob.getHomeAnchor();
        final Location current = mob.getEntity().getLocation();
        if (!current.getWorld().equals(home.getWorld())) {
            return true;
        }
        return current.distanceSquared(home) > leashRange * leashRange;
    }

    @Override
    public boolean shouldContinue() {
        final Location home = mob.getHomeAnchor();
        final Location current = mob.getEntity().getLocation();
        if (!current.getWorld().equals(home.getWorld())) {
            return true;
        }
        final double arrival = leashRange * arrivalFactor;
        return current.distanceSquared(home) > arrival * arrival;
    }

    @Override
    public void tick() {
        mob.setCurrentTarget(null);
        mob.startMoving(mob.getHomeAnchor(), returnSpeed);
    }

    @Override
    public void stop() {
        mob.stopMoving();
    }

}
