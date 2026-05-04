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
     * Composition order (see {@code RuleLayer}):
     * <ol>
     *   <li>Base = tool {@code ToolComponent} match, else vanilla destroy-speed fallback.</li>
     *   <li>If any matching rule (tool or global) is unbreakable → unbreakable wins.</li>
     *   <li>If any global {@code OVERRIDE} matches → that rule's speed replaces the result;
     *       highest {@code priority()} wins, ties go to last-registered.</li>
     *   <li>Otherwise: {@code (base + Σ additive) × Π multiplicative}, clamped to {@code MIN_SPEED}.</li>
     * </ol>
     */
    @NotNull
    BlockBreakProperties resolve(@NotNull Player player, @NotNull Block block, @Nullable ItemStack held);
}
