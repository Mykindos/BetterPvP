package me.mykindos.betterpvp.core.framework.blockbreak.resolver;

import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakProperties;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BlockBreakResolver {

    /**
     * Compute the effective break properties for {@code player} breaking
     * {@code block} while holding {@code held} (may be null/air).
     * <p>
     * Composition rules:
     * <ul>
     *   <li>Tool rule found, no global rule → tool only (no stacking).</li>
     *   <li>Tool rule found, global rule found → additive merge of speeds; either unbreakable wins.</li>
     *   <li>No tool rule, global rule found → global only.</li>
     *   <li>Neither → vanilla fallback (uses {@code Material.getHardness()} and a hand-equivalent speed).</li>
     * </ul>
     */
    @NotNull
    BlockBreakProperties resolve(@NotNull Player player, @NotNull Block block, @Nullable ItemStack held);
}
