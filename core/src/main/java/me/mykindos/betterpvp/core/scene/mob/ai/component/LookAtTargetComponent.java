package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.EnumSet;

/**
 * Keeps the mob's head/body oriented towards its current target. Pure look behaviour, so it
 * can run alongside any non-LOOK movement component.
 */
public class LookAtTargetComponent implements AIComponent {

    private final SceneMob mob;

    public LookAtTargetComponent(SceneMob mob) {
        this.mob = mob;
    }

    @Override
    public EnumSet<AIControl> getControls() {
        return EnumSet.of(AIControl.LOOK);
    }

    @Override
    public boolean canStart() {
        return mob.getCurrentTarget() != null;
    }

    @Override
    public void tick() {
        final LivingEntity target = mob.getCurrentTarget();
        if (target == null) {
            return;
        }

        final Mob bukkitMob = mob.getBukkitMob();
        if (bukkitMob != null) {
            bukkitMob.lookAt(target.getEyeLocation());
        }
    }

}
