package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import me.mykindos.betterpvp.core.scene.mob.target.TargetSelector;

import java.util.EnumSet;

/**
 * Continuously maintains the mob's current target via a pluggable {@link TargetSelector}.
 * Runs every tick: it first drops a stale target (dead/removed/different world) and then asks
 * the selector for the best candidate, which may be {@code null} if nothing qualifies.
 */
public class TargetingComponent implements AIComponent {

    private final SceneMob mob;
    private final TargetSelector selector;

    public TargetingComponent(SceneMob mob, TargetSelector selector) {
        this.mob = mob;
        this.selector = selector;
    }

    @Override
    public EnumSet<AIControl> getControls() {
        return EnumSet.of(AIControl.TARGET);
    }

    @Override
    public boolean canStart() {
        return true;
    }

    @Override
    public void tick() {
        if (!mob.isValidTarget(mob.getCurrentTarget())) {
            mob.setCurrentTarget(null);
        }

        mob.setCurrentTarget(selector.select(mob).orElse(null));
    }

}
