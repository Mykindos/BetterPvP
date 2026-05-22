package me.mykindos.betterpvp.core.block.impl.anvil.operation;

import me.mykindos.betterpvp.core.item.ItemInstance;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Strategy describing what an anvil does when its hammer-swing progress completes.
 * <br>
 * The anvil mechanics (swing timing, displays, persistence) are agnostic to whether
 * the current job is crafting a recipe or repairing an item — they only ever talk to
 * this interface. Each concrete operation owns its own progress text and completion
 * side effects.
 *
 * @see CraftOperation
 * @see RepairOperation
 */
public interface AnvilOperation {

    /**
     * @return the number of hammer swings required to complete this operation.
     */
    int requiredSwings();

    /**
     * @return whether enough swings have been performed to complete this operation.
     */
    default boolean isReady(int currentSwings) {
        return currentSwings >= requiredSwings();
    }

    /**
     * @return progress towards completion as a value in {@code [0.0, 1.0]}.
     */
    default float progress(int currentSwings) {
        final int required = requiredSwings();
        return required <= 0 ? 0.0f : Math.min(1.0f, (float) currentSwings / required);
    }

    /**
     * The text shown on the hologram above the anvil for the given swing count.
     *
     * @return the component to render (may be {@link Component#empty()} to show nothing).
     */
    @NotNull Component hologramText(int currentSwings);

    /**
     * Performs the operation's side effects (consuming items, producing/repairing,
     * dropping results) and returns the items that should remain on the anvil.
     *
     * @param player   the player who completed the operation
     * @param items    slot → item instance currently on the anvil
     * @param location the anvil location (drop point for any results)
     * @return the items remaining on the anvil after completion
     */
    @NotNull List<ItemInstance> complete(@NotNull Player player,
                                         @NotNull Map<Integer, ItemInstance> items,
                                         @NotNull Location location);
}
