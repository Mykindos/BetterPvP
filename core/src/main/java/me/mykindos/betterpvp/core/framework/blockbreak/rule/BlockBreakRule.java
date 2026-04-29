package me.mykindos.betterpvp.core.framework.blockbreak.rule;

import me.mykindos.betterpvp.core.framework.blockbreak.rule.matcher.CompositeMatcher;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * The atomic unit: a matcher + the properties that apply to blocks it matches.
 * Operations between rules (AND/OR/...) live on the {@link BlockMatcher} side.
 *
 * @see CompositeMatcher
 */
public interface BlockBreakRule {

    Predicate<Player> ALWAYS = p -> true;

    @NotNull BlockMatcher matcher();

    @NotNull BlockBreakProperties properties();

    /**
     * Optional per-player gate evaluated at resolve time. A rule whose condition
     * returns {@code false} is treated as if its matcher did not match, so other
     * rules in the same registry are still considered.
     */
    default @NotNull Predicate<Player> condition() {
        return ALWAYS;
    }

    static BlockBreakRule of(@NotNull BlockMatcher matcher, @NotNull BlockBreakProperties properties) {
        return new SimpleBlockBreakRule(matcher, properties, ALWAYS);
    }

    static BlockBreakRule of(@NotNull BlockMatcher matcher,
                             @NotNull BlockBreakProperties properties,
                             @NotNull Predicate<Player> condition) {
        return new SimpleBlockBreakRule(matcher, properties, condition);
    }
}
