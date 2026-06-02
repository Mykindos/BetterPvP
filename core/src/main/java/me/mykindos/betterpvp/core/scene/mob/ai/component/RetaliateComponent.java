package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Range;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Drives a NEUTRAL mob to retaliate against whatever has built up the most threat against it.
 * The current target is resolved fresh from the {@link me.mykindos.betterpvp.core.scene.mob.target.ThreatTable}
 * each tick, and threat decays gradually so the mob eventually disengages once attacks stop.
 */
@Accessors(fluent = true, chain = true)
public class RetaliateComponent implements AIComponent {

    private final SceneMob mob;

    /** Threat removed from every attacker each tick; higher values make the mob disengage sooner. */
    @Setter
    @Range(from = 0, to = Long.MAX_VALUE)
    private double threatDecay = 0.5;

    public RetaliateComponent(SceneMob mob) {
        this.mob = mob;
    }

    @Override
    public EnumSet<AIControl> getControls() {
        return EnumSet.of(AIControl.TARGET);
    }

    @Override
    public boolean canStart() {
        return !mob.getThreat().isEmpty();
    }

    @Override
    public boolean shouldContinue() {
        return !mob.getThreat().isEmpty();
    }

    @Override
    public void tick() {
        mob.getThreat().decay(threatDecay);

        final Optional<UUID> highest = mob.getThreat().highest();
        if (highest.isEmpty()) {
            return;
        }

        final Entity entity = Bukkit.getEntity(highest.get());
        if (entity instanceof LivingEntity living && mob.isValidTarget(living)) {
            mob.setCurrentTarget(living);
        } else if (entity instanceof LivingEntity stale) {
            mob.getThreat().remove(stale);
        }
    }

    @Override
    public void stop() {
        mob.setCurrentTarget(null);
    }

}
