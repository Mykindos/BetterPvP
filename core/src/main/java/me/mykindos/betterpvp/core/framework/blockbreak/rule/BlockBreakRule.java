package me.mykindos.betterpvp.core.framework.blockbreak.rule;

import me.mykindos.betterpvp.core.framework.blockbreak.rule.matcher.CompositeMatcher;
import org.jetbrains.annotations.NotNull;

/**
 * The atomic unit: a matcher + the properties that apply to blocks it matches.
 * Operations between rules (AND/OR/...) live on the {@link BlockMatcher} side.
 *
 * @see CompositeMatcher
 */
public interface BlockBreakRule {

    @NotNull BlockMatcher matcher();

    @NotNull BlockBreakProperties properties();

    static BlockBreakRule of(@NotNull BlockMatcher matcher, @NotNull BlockBreakProperties properties) {
        return new SimpleBlockBreakRule(matcher, properties);
    }
}
