package me.mykindos.betterpvp.core.access;

import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Provides access requirements for items on behalf of a gating source (e.g. skill tree, dungeon progression).
 *
 * <p>Implementations are registered with {@link ItemAccessService} and consulted whenever the system
 * needs to determine whether a player may interact with an item in a given {@link AccessScope}.</p>
 */
public interface ItemAccessProvider {

    /**
     * Evaluate whether this provider has an access requirement for the given player and item.
     *
     * <p>When {@code player} is {@code null} (lore rendering without a viewer context), implementations
     * should return the requirement structure with {@code satisfied=false} if the item is gated by this
     * provider, so lore lines can still be rendered.</p>
     *
     * @param player  the player attempting access, or {@code null} when called for lore rendering only
     * @param itemKey the adventure Key of the item being accessed
     * @return an {@link AccessRequirement} if this provider gates the item, or {@link Optional#empty()} otherwise
     */
    Optional<AccessRequirement> evaluate(Player player, Key itemKey);
}
