package me.mykindos.betterpvp.core.framework.blockbreak.global;

import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakRule;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-player global block-break rules. These layer on top of any rule supplied
 * by the held tool's {@code ToolComponent}.
 */
public interface GlobalBlockBreakRules {

    /** Adds a global rule for the player. Conflict policy mirrors {@code ToolComponent}. */
    void addRule(@NotNull UUID playerId, @NotNull BlockBreakRule rule);

    void removeRule(@NotNull UUID playerId, @NotNull BlockBreakRule rule);

    void clear(@NotNull UUID playerId);

    @NotNull List<BlockBreakRule> getRules(@NotNull UUID playerId);

    Optional<BlockBreakRule> resolve(@NotNull UUID playerId, @NotNull Block block);
}
