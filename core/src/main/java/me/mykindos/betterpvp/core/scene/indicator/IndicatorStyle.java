package me.mykindos.betterpvp.core.scene.indicator;

import me.mykindos.betterpvp.core.scene.SceneObject;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * What an indicator looks like — the one piece that changes when a floating item becomes a modeled entity.
 * <p>
 * Everything else about an indicator (following its target, who can see it, cleaning up when the target goes away) is
 * the same whatever it is made of, so it lives in {@link IndicatorService} and never has to be rewritten. Swapping a
 * marker for a modeled arrow is a different style, not a different system.
 */
@FunctionalInterface
public interface IndicatorStyle {

    /**
     * Spawns the indicator's body and returns it bound and ready.
     * <p>
     * Bound eagerly rather than chunk-managed: an indicator follows something that moves, and chunk-managed
     * materialization keys off a frozen anchor — the same reason wandering mobs were never migrated to it.
     *
     * @param at where it should first appear
     */
    @NotNull SceneObject create(@NotNull Location at);
}
