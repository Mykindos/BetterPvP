package me.mykindos.betterpvp.core.scene.mob.target;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import org.bukkit.entity.LivingEntity;

import java.util.Optional;

/**
 * Strategy for picking a target for a {@link SceneMob}. Implementations decide <em>who</em> the
 * mob should focus (nearest enemy, lowest-health enemy, highest-threat attacker, ...). The
 * {@code TargetingComponent} runs a selector each tick and writes the result to
 * {@link SceneMob#setCurrentTarget(LivingEntity)}; attack/look components then read it.
 * <p>
 * Concrete selectors are provided as composable factories in {@code TargetSelectors}.
 */
@FunctionalInterface
public interface TargetSelector {

    /**
     * @return the chosen target, or empty if no valid target exists right now.
     */
    Optional<LivingEntity> select(SceneMob mob);

}
