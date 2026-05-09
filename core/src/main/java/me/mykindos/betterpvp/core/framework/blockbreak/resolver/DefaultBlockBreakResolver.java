package me.mykindos.betterpvp.core.framework.blockbreak.resolver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.block.SmartBlockBreakOverride;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockOverrides;
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
    private final SmartBlockFactory smartBlockFactory;

    @Inject
    public DefaultBlockBreakResolver(ItemFactory itemFactory, GlobalBlockBreakRules globalRules,
                                     SmartBlockFactory smartBlockFactory) {
        this.itemFactory = itemFactory;
        this.globalRules = globalRules;
        this.smartBlockFactory = smartBlockFactory;
    }

    @Override
    public @NotNull BlockBreakProperties resolve(@NotNull Player player, @NotNull Block block, @Nullable ItemStack held) {
        final SmartBlockBreakOverride smartOverride = SmartBlockOverrides.resolve(smartBlockFactory, block, player, held);
        if (smartOverride.unbreakable()) return BlockBreakProperties.unbreakable();

        final BlockBreakProperties tool = resolveFromTool(held, block).orElse(null);
        final List<BlockBreakRule> globals = globalRules.resolveAll(player, block);

        if (tool != null && !tool.isBreakable()) return BlockBreakProperties.unbreakable();
        for (BlockBreakRule rule : globals) {
            if (!rule.properties().isBreakable()) return BlockBreakProperties.unbreakable();
        }

        // Sort once, then walk in three passes (additive → multiplicative → override).
        // Within each layer, highest priority is applied first; ties preserve registration order.
        final List<BlockBreakRule> ordered = new java.util.ArrayList<>(globals);
        ordered.sort(java.util.Comparator
                .comparingInt((BlockBreakRule r) -> r.layer().ordinal())
                .thenComparing(java.util.Comparator.comparingInt(BlockBreakRule::priority).reversed()));

        long additiveSum = 0L;
        double multiplierProduct = 1.0;
        BlockBreakRule winningOverride = null;
        for (BlockBreakRule rule : ordered) {
            switch (rule.layer()) {
                case ADDITIVE -> additiveSum += rule.properties().getBreakSpeed();
                case MULTIPLICATIVE -> multiplierProduct *= rule.properties().getMultiplier();
                case OVERRIDE -> {
                    // First in this pass wins because the list is pre-sorted by priority desc.
                    if (winningOverride == null) winningOverride = rule;
                }
            }
        }

        // Fold SmartBlock multipliers into the same product. speedMultiplier always applies;
        // toolSpeedMultiplier applies only when the requiredTool predicate matches the held
        // item — or unconditionally when the predicate is absent (Nexo's defaults arrive
        // pre-resolved, so they take this path).
        if (smartOverride.speedMultiplier().isPresent()) {
            multiplierProduct *= smartOverride.speedMultiplier().getAsDouble();
        }
        if (smartOverride.toolSpeedMultiplier().isPresent()) {
            final boolean toolMatches = smartOverride.requiredTool()
                    .map(p -> held != null && p.test(held))
                    .orElse(true);
            if (toolMatches) {
                multiplierProduct *= smartOverride.toolSpeedMultiplier().getAsDouble();
            }
        }

        if (winningOverride != null) {
            return BlockBreakProperties.breakable(
                    Math.max(BlockBreakProperties.MIN_SPEED, winningOverride.properties().getBreakSpeed()));
        }

        final int base = tool != null ? tool.getBreakSpeed() : vanillaFallbackSpeed(block, held);
        if (additiveSum == 0L && multiplierProduct == 1.0) {
            return BlockBreakProperties.breakable(Math.max(BlockBreakProperties.MIN_SPEED, base));
        }

        final double combined = ((double) base + (double) additiveSum) * multiplierProduct;
        final int saturated = combined > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.round(combined);
        return BlockBreakProperties.breakable(Math.max(BlockBreakProperties.MIN_SPEED, saturated));
    }

    private Optional<BlockBreakProperties> resolveFromTool(@Nullable ItemStack held, Block block) {
        if (held == null || held.getType().isAir()) return Optional.empty();
        final Optional<ItemInstance> instOpt = itemFactory.fromItemStack(held);
        if (instOpt.isEmpty()) return Optional.empty();
        final Optional<ToolComponent> compOpt = instOpt.get().getComponent(ToolComponent.class);
        if (compOpt.isEmpty()) return Optional.empty();
        return compOpt.get().resolve(block).map(BlockBreakRule::properties);
    }

    /**
     * True vanilla fallback when the held item has no {@code ToolComponent} match.
     * Uses Paper's destroy-speed lookup so a diamond pick on dirt is fast and a fist
     * on stone is slow. Drops are governed by {@code breakNaturally(held)} downstream,
     * which already respects {@code NEEDS_*_TOOL} tags.
     */
    private int vanillaFallbackSpeed(Block block, @Nullable ItemStack held) {
        final ItemStack effectiveHeld = held == null ? new ItemStack(org.bukkit.Material.AIR) : held;
        float destroySpeed = block.getDestroySpeed(effectiveHeld);
        if (!block.isPreferredTool(effectiveHeld)) {
            destroySpeed /= (100f / 30f);
        }
        return Math.max(BlockBreakProperties.MIN_SPEED,
                Math.round(destroySpeed * (float) ToolMiningSpeed.SCALE));
    }
}
