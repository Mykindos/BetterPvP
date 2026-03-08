package me.mykindos.betterpvp.core.interaction.condition;

import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import org.jetbrains.annotations.NotNull;

/**
 * A functional interface representing a condition that must be met for an interaction to execute.
 * Conditions can be combined using {@link #and(InteractionCondition)}, {@link #or(InteractionCondition)},
 * and {@link #negate()}.
 */
@FunctionalInterface
public interface InteractionCondition {

    /**
     * Check if this condition is met.
     *
     * @param actor   the actor attempting the interaction
     * @param context the interaction context
     * @return the result of the condition check
     */
    ConditionResult check(@NotNull InteractionActor actor, @NotNull InteractionContext context);

    /**
     * Create a condition that requires both this condition and another to pass.
     *
     * @param other the other condition
     * @return a combined condition
     */
    default InteractionCondition and(@NotNull InteractionCondition other) {
        return (actor, context) -> {
            ConditionResult result = this.check(actor, context);
            if (!result.passed()) {
                return result;
            }
            return other.check(actor, context);
        };
    }

    /**
     * Create a condition that requires either this condition or another to pass.
     *
     * @param other the other condition
     * @return a combined condition
     */
    default InteractionCondition or(@NotNull InteractionCondition other) {
        return (actor, context) -> {
            ConditionResult result = this.check(actor, context);
            if (result.passed()) {
                return result;
            }
            return other.check(actor, context);
        };
    }

    /**
     * Create a condition that inverts this condition's result.
     *
     * @return an inverted condition
     */
    default InteractionCondition negate() {
        return (actor, context) -> {
            ConditionResult result = this.check(actor, context);
            if (result.passed()) {
                return ConditionResult.fail();
            }
            return ConditionResult.success();
        };
    }
}
