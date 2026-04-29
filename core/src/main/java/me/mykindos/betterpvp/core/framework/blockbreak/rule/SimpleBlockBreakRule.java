package me.mykindos.betterpvp.core.framework.blockbreak.rule;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

record SimpleBlockBreakRule(@NotNull BlockMatcher matcher,
                            @NotNull BlockBreakProperties properties,
                            @NotNull Predicate<Player> condition) implements BlockBreakRule {
}
