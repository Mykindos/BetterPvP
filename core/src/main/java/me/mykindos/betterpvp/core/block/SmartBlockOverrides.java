package me.mykindos.betterpvp.core.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Static helpers for resolving the merged {@link SmartBlockBreakOverride} for a block
 * interaction. Used by the block-break resolver (speed/breakability) and the tick loop
 * (hardness).
 */
public final class SmartBlockOverrides {

    private SmartBlockOverrides() {}

    /**
     * Resolves the merged smart-block override for this interaction:
     * <ol>
     *   <li>The {@link SmartBlock}'s {@link SmartBlock#getBreakOverride} return value wins where set.</li>
     *   <li>Absent fields fall back to provider defaults supplied by the factory via
     *       {@link SmartBlockFactory#getBreakOverrideDefaults} (e.g. Nexo's {@code Breakable}).</li>
     *   <li>Anything still absent stays absent — callers fall through to vanilla.</li>
     * </ol>
     * Returns {@link SmartBlockBreakOverride#empty()} when the block isn't a smart block.
     */
    public static @NotNull SmartBlockBreakOverride resolve(@NotNull SmartBlockFactory factory,
                                                            @NotNull Block block,
                                                            @NotNull Player player,
                                                            @Nullable ItemStack held) {
        final Optional<SmartBlockInstance> instOpt = factory.from(block);
        if (instOpt.isEmpty()) return SmartBlockBreakOverride.empty();

        final SmartBlockInstance inst = instOpt.get();
        final ItemStack effectiveHeld = held == null ? new ItemStack(Material.AIR) : held;
        final SmartBlockBreakOverride own = inst.getType().getBreakOverride(inst, player, effectiveHeld);
        return own.merge(factory.getBreakOverrideDefaults(inst, player, effectiveHeld));
    }
}
