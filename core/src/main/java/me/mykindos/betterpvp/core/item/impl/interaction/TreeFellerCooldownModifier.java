package me.mykindos.betterpvp.core.item.impl.interaction;

import org.bukkit.entity.Player;

/**
 * Optional hook that lets an external module (e.g. the progression skill tree) reduce
 * the per-player Tree Feller cooldown below its configured base value.
 *
 * Register an implementation via {@link TreeFellerInteraction#setModifier(TreeFellerCooldownModifier)}.
 */
@FunctionalInterface
public interface TreeFellerCooldownModifier {

    /**
     * Returns the effective cooldown in seconds for the given player.
     *
     * @param player       the player about to receive the cooldown
     * @param baseCooldown the configured base cooldown in seconds
     * @return effective cooldown in seconds (must be >= 0)
     */
    double getEffectiveCooldown(Player player, double baseCooldown);
}
