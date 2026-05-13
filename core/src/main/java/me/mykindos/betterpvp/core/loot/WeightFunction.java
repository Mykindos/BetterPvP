package me.mykindos.betterpvp.core.loot;

import me.mykindos.betterpvp.core.loot.expression.ExpressionEngine;

import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Computes the weight of a {@link WeightedEntry} for a given {@link LootContext}.
 * <p>
 * Weight is evaluated per roll. Non-positive results disable the entry for that roll.
 */
@FunctionalInterface
public interface WeightFunction extends ToIntFunction<LootContext> {

    /**
     * A weight function that always returns {@code weight}.
     */
    static WeightFunction constant(int weight) {
        return ctx -> weight;
    }

    /**
     * A weight function that evaluates {@code expression} as a number. {@code fallback}
     * is used when evaluation fails.
     */
    static WeightFunction expression(String expression, int fallback) {
        return ctx -> {
            double v = ExpressionEngine.evalDouble(expression, ctx, Map.of(), fallback);
            return (int) Math.max(0, Math.round(v));
        };
    }
}
