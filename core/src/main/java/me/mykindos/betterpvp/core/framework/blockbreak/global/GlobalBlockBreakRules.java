package me.mykindos.betterpvp.core.framework.blockbreak.global;

import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakRule;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-player global block-break rules. These layer on top of any rule supplied
 * by the held tool's {@code ToolComponent}.
 */
public interface GlobalBlockBreakRules {

    /**
     * Adds a global rule for the player. Multiple rules with overlapping matchers may
     * coexist — at resolve time their {@link BlockBreakRule#properties()} stack
     * additively via {@code BlockBreakProperties.merge}.
     */
    void addRule(@NotNull UUID playerId, @NotNull BlockBreakRule rule);

    void removeRule(@NotNull UUID playerId, @NotNull BlockBreakRule rule);

    void clear(@NotNull UUID playerId);

    @NotNull List<BlockBreakRule> getRules(@NotNull UUID playerId);

    /**
     * Resolves the first matching rule for {@code player} on {@code block}. A rule
     * matches when its matcher accepts the block <em>and</em> its condition predicate
     * accepts the player; rules failing either are skipped.
     */
    Optional<BlockBreakRule> resolve(@NotNull Player player, @NotNull Block block);

    /**
     * Resolves every rule for {@code player} that matches {@code block} and whose
     * condition accepts the player. Returned in registration order. Used by the
     * resolver to additively stack overlapping global rules.
     */
    @NotNull List<BlockBreakRule> resolveAll(@NotNull Player player, @NotNull Block block);
}
