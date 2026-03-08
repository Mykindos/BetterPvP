package me.mykindos.betterpvp.core.interaction;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the result of executing an interaction.
 * <p>
 * Results are either complete (Success, Fail) or incomplete (Running).
 * Running interactions will be ticked by the interaction executor until
 * they complete or timeout.
 */
public sealed interface InteractionResult permits
        InteractionResult.Success,
        InteractionResult.Running,
        InteractionResult.Fail {

    /**
     * @return true if the interaction has completed (success or fail)
     */
    boolean isComplete();

    /**
     * @return true if the chain should advance to the next node
     */
    boolean shouldAdvanceChain();

    /**
     * @return true if the interaction was successful
     */
    boolean isSuccess();

    /**
     * Represents a successful completion of an interaction.
     *
     * @param advanceChain whether the chain should advance to the next node
     */
    record Success(boolean advanceChain) implements InteractionResult {
        /**
         * Success result that advances the chain to the next node.
         */
        public static final Success ADVANCE = new Success(true);

        /**
         * Success result that does not advance the chain.
         * Useful for interactions that need to stay at the current node.
         */
        public static final Success NO_ADVANCE = new Success(false);

        @Override
        public boolean isComplete() {
            return true;
        }

        @Override
        public boolean shouldAdvanceChain() {
            return advanceChain;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }
    }

    /**
     * Represents an interaction that is still running and needs to be ticked.
     * <p>
     * The interaction executor will continue calling execute() at the specified
     * interval until a complete result is returned or the max runtime is exceeded.
     *
     * @param maxRuntimeMillis maximum time this interaction can run before timeout
     * @param intervalTicks    how often to tick the interaction (minimum 1 tick = 50ms)
     * @param gracefulTimeout  if true, timeout completes as Success instead of Fail
     */
    record Running(long maxRuntimeMillis, int intervalTicks, boolean gracefulTimeout) implements InteractionResult {
        /**
         * Creates a running result that ticks every game tick with graceful timeout.
         * When the runtime expires, the interaction completes successfully.
         *
         * @param maxRuntimeMillis maximum runtime in milliseconds
         */
        public Running(long maxRuntimeMillis) {
            this(maxRuntimeMillis, 1, true);
        }

        /**
         * Creates a running result with specified interval and graceful timeout.
         *
         * @param maxRuntimeMillis maximum runtime in milliseconds
         * @param intervalTicks    how often to tick (minimum 1)
         */
        public Running(long maxRuntimeMillis, int intervalTicks) {
            this(maxRuntimeMillis, intervalTicks, true);
        }

        /**
         * Creates a running result with validated parameters.
         */
        public Running {
            if (intervalTicks < 1) {
                intervalTicks = 1; // Minimum 1 tick
            }
            if (maxRuntimeMillis < 0) {
                maxRuntimeMillis = 0;
            }
        }

        /**
         * Creates a running result that will fail on timeout.
         * Use this for interactions where timeout indicates something went wrong.
         *
         * @param maxRuntimeMillis maximum runtime in milliseconds
         * @param intervalTicks    how often to tick (minimum 1)
         * @return a Running result that fails on timeout
         */
        public static Running withFailOnTimeout(long maxRuntimeMillis, int intervalTicks) {
            return new Running(maxRuntimeMillis, intervalTicks, false);
        }

        /**
         * Creates a running result that will fail on timeout, ticking every game tick.
         *
         * @param maxRuntimeMillis maximum runtime in milliseconds
         * @return a Running result that fails on timeout
         */
        public static Running withFailOnTimeout(long maxRuntimeMillis) {
            return new Running(maxRuntimeMillis, 1, false);
        }

        @Override
        public boolean isComplete() {
            return false;
        }

        @Override
        public boolean shouldAdvanceChain() {
            return false;
        }

        @Override
        public boolean isSuccess() {
            return false; // Not yet determined
        }
    }

    /**
     * Represents a failed interaction.
     *
     * @param reason  the reason for failure
     * @param message optional custom message (may be null)
     */
    record Fail(@NotNull FailReason reason, String message) implements InteractionResult {

        /**
         * Creates a fail result with just a reason.
         */
        public Fail(@NotNull FailReason reason) {
            this(reason, null);
        }

        @Override
        public boolean isComplete() {
            return true;
        }

        @Override
        public boolean shouldAdvanceChain() {
            return false;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }
    }

    /**
     * Reasons for interaction failure.
     */
    enum FailReason {
        /**
         * Conditions for the interaction were not met.
         */
        CONDITIONS,

        /**
         * The interaction is on cooldown.
         */
        COOLDOWN,

        /**
         * Insufficient energy to perform the interaction.
         */
        ENERGY,

        /**
         * The interaction was cancelled by an external source.
         */
        CANCELLED,

        /**
         * The interaction timed out (exceeded max runtime).
         */
        TIMEOUT,

        /**
         * A custom failure reason.
         */
        CUSTOM
    }
}
