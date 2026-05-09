package me.mykindos.betterpvp.core.block;

import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a custom block type with specific behaviors and properties.
 */
public abstract class SmartBlock {

    private final String id;
    private final String name;

    protected SmartBlock(@NotNull String id, @NotNull String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Composite of break-related overrides for this block, evaluated for a specific
     * interaction (player + held item + instance state). Default implementation returns
     * {@link SmartBlockBreakOverride#empty()} — every field absent.
     *
     * <p>Resolution order in {@code DefaultBlockBreakResolver}:
     * <ol>
     *   <li>Fields supplied here win.</li>
     *   <li>If this {@code SmartBlock} also implements {@code NexoBlock}, fields absent
     *       here are filled from Nexo's {@code Breakable} (hardness, multipliers).</li>
     *   <li>Anything still absent falls back to vanilla / global rules.</li>
     * </ol>
     *
     * <p>Subclasses should typically override this method via the builder, e.g.
     * {@code return SmartBlockBreakOverride.builder().hardness(5.0).build();} — and rely
     * on the merge with Nexo defaults for everything else.
     */
    public @NotNull SmartBlockBreakOverride getBreakOverride(@NotNull SmartBlockInstance instance,
                                                              @NotNull Player player,
                                                              @NotNull ItemStack held) {
        return SmartBlockBreakOverride.empty();
    }

    /**
     * Handles a click action on the block instance by a player.
     * @param blockInstance the instance of the block being clicked
     * @param player the player who clicked the block
     * @param action the action performed by the player (e.g., right-click, left-click)
     * @return true if the action was handled, false otherwise
     */
    public boolean handleClick(@NotNull SmartBlockInstance blockInstance, @NotNull Player player, @NotNull Action action) {
        // ignore
        return false;
    }

    /**
     * @return the name of the block
     */
    public String getName() {
        return name;
    }

    /**
     * @return the namespaced key of the block
     */
    public String getKey() {
        return id;
    }

}
