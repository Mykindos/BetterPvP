package me.mykindos.betterpvp.core.loot;

import me.mykindos.betterpvp.core.loot.expression.ExpressionEngine;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Factories for {@link Loot#getCondition()} predicates.
 */
public final class LootConditions {

    private LootConditions() {
    }

    /**
     * A predicate that always returns {@code true}.
     */
    public static Predicate<LootContext> always() {
        return ctx -> true;
    }

    /**
     * A predicate that evaluates {@code expression} as a boolean against the context.
     * Returns {@code false} if the expression fails to evaluate.
     */
    public static Predicate<LootContext> expression(String expression) {
        return ctx -> ExpressionEngine.evalBoolean(expression, ctx, Map.of(), false);
    }
}
