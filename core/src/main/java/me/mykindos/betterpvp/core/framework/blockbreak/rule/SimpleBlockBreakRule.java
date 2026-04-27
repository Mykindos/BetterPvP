package me.mykindos.betterpvp.core.framework.blockbreak.rule;

import org.jetbrains.annotations.NotNull;

record SimpleBlockBreakRule(@NotNull BlockMatcher matcher,
                            @NotNull BlockBreakProperties properties) implements BlockBreakRule {
}
