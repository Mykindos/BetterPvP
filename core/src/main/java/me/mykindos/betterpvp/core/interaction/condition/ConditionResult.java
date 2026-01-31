package me.mykindos.betterpvp.core.interaction.condition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of an interaction condition check.
 *
 * @param passed true if the condition passed
 * @param failureMessage optional message explaining why the condition failed
 */
public record ConditionResult(boolean passed, @Nullable String failureMessage) {

    private static final ConditionResult SUCCESS = new ConditionResult(true, null);

    /**
     * Create a successful result.
     *
     * @return a successful result
     */
    public static ConditionResult success() {
        return SUCCESS;
    }

    /**
     * Create a failed result with a message.
     *
     * @param message the failure message
     * @return a failed result
     */
    public static ConditionResult fail(@NotNull String message) {
        return new ConditionResult(false, message);
    }

    /**
     * Create a failed result without a message.
     *
     * @return a failed result
     */
    public static ConditionResult fail() {
        return new ConditionResult(false, null);
    }
}
