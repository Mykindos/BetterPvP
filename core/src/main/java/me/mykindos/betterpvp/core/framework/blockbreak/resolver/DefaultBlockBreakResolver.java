package me.mykindos.betterpvp.core.framework.blockbreak.resolver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blockbreak.ToolMiningSpeed;
import me.mykindos.betterpvp.core.framework.blockbreak.component.ToolComponent;
import me.mykindos.betterpvp.core.framework.blockbreak.global.GlobalBlockBreakRules;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakProperties;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakRule;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@Singleton
public class DefaultBlockBreakResolver implements BlockBreakResolver {

    private final ItemFactory itemFactory;
    private final GlobalBlockBreakRules globalRules;

    @Inject
    public DefaultBlockBreakResolver(ItemFactory itemFactory, GlobalBlockBreakRules globalRules) {
        this.itemFactory = itemFactory;
        this.globalRules = globalRules;
    }

    @Override
    public @NotNull BlockBreakProperties resolve(@NotNull Player player, @NotNull Block block, @Nullable ItemStack held) {
        final BlockBreakProperties tool = resolveFromTool(held, block).orElse(null);

        // Fold all matching globals so multiple rules (e.g. an item buff plus a
        // passive profession attribute) can both contribute to the player's speed.
        final List<BlockBreakRule> globals = globalRules.resolveAll(player, block);
        BlockBreakProperties global = null;
        for (BlockBreakRule rule : globals) {
            final BlockBreakProperties props = rule.properties();
            global = global == null ? props : global.merge(props);
        }

        // Either side unbreakable → unbreakable.
        if ((tool != null && !tool.isBreakable()) || (global != null && !global.isBreakable())) {
            return BlockBreakProperties.unbreakable();
        }

        if (tool != null && global != null) {
            return tool.merge(global); // additive
        }
        if (tool != null) return tool;       // no stacking when no global match
        if (global != null) return global;   // no stacking when no tool match

        // Neither matched — true vanilla fallback. Use Paper's real tool/block speed
        // so a diamond pickaxe on dirt still mines fast, and a fist on stone is slow.
        // Drops are governed by breakNaturally(held) downstream, which already respects
        // NEEDS_*_TOOL tags (obsidian + wooden pickaxe drops nothing).
        final ItemStack effectiveHeld = held == null ? new ItemStack(org.bukkit.Material.AIR) : held;
        float destroySpeed = block.getDestroySpeed(effectiveHeld);
        // Vanilla applies a ~3.33x slowdown when the tool isn't the preferred tier
        // (the 30 → 100 divisor in the wiki formula). Bake it into the speed here.
        if (!block.isPreferredTool(effectiveHeld)) {
            destroySpeed /= (100f / 30f);
        }
        final int scaled = Math.max(BlockBreakProperties.MIN_SPEED,
                Math.round(destroySpeed * (float) ToolMiningSpeed.SCALE));
        return BlockBreakProperties.breakable(scaled);
    }

    private Optional<BlockBreakProperties> resolveFromTool(@Nullable ItemStack held, Block block) {
        if (held == null || held.getType().isAir()) return Optional.empty();
        final Optional<ItemInstance> instOpt = itemFactory.fromItemStack(held);
        if (instOpt.isEmpty()) return Optional.empty();
        final Optional<ToolComponent> compOpt = instOpt.get().getComponent(ToolComponent.class);
        if (compOpt.isEmpty()) return Optional.empty();
        return compOpt.get().resolve(block).map(BlockBreakRule::properties);
    }
}
