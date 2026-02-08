package me.mykindos.betterpvp.core.interaction.state;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks an actor's position in an interaction chain.
 */
@Getter
public class InteractionState {

    /**
     * The root node that started this chain.
     * Used to track cooldowns per-root rather than per-chain.
     */
    @Setter
    @Nullable
    private InteractionChainNode rootNode;

    /**
     * The current node in the chain (null if at root level).
     */
    @Setter
    @Nullable
    private InteractionChainNode currentNode;

    /**
     * Counter for multi-input requirements.
     */
    @Setter
    private int inputCounter;

    /**
     * Timestamp of the last interaction in this chain.
     */
    @Setter
    private long lastInteractionTime;

    /**
     * Shared context data across the chain.
     */
    private final InteractionContext context;

    public InteractionState() {
        this.rootNode = null;
        this.currentNode = null;
        this.inputCounter = 0;
        this.lastInteractionTime = System.currentTimeMillis();
        this.context = new InteractionContext();
    }

    /**
     * Advance to a new node in the chain.
     *
     * @param node the new current node
     */
    public void advanceTo(@NotNull InteractionChainNode node) {
        this.currentNode = node;
        this.inputCounter = 0;
        this.lastInteractionTime = System.currentTimeMillis();
    }

    /**
     * Increment the input counter.
     *
     * @return the new counter value
     */
    public int incrementInputCounter() {
        return ++inputCounter;
    }

    /**
     * Reset the state to the root level.
     * This resets the context for a new chain (clears all data and reinitializes chain start time).
     */
    public void reset() {
        this.rootNode = null;
        this.currentNode = null;
        this.inputCounter = 0;
        this.context.reset();
    }

    /**
     * Check if this state is currently active in a chain (not at root).
     *
     * @return true if in an active chain
     */
    public boolean isInChain() {
        return currentNode != null;
    }

    /**
     * Check if the state has timed out based on the current node's timeout.
     *
     * @return true if timed out
     */
    public boolean hasTimedOut() {
        if (currentNode == null) {
            return false;
        }
        long timeout = currentNode.getTimeoutMillis(context);
        if (timeout <= 0) {
            return false;
        }
        return System.currentTimeMillis() - lastInteractionTime > timeout;
    }

    /**
     * Check if the minimum delay has passed since the last interaction.
     * This is used to prevent inputs from being processed too quickly.
     *
     * @param minimumDelayMillis the minimum delay required in milliseconds
     * @return true if the minimum delay has passed or if no delay is required
     */
    public boolean hasMinimumDelayPassed(long minimumDelayMillis) {
        if (minimumDelayMillis <= 0) {
            return true;
        }
        return System.currentTimeMillis() - lastInteractionTime >= minimumDelayMillis;
    }

    /**
     * Update the last interaction time to now.
     */
    public void touch() {
        this.lastInteractionTime = System.currentTimeMillis();
    }

    /**
     * Get the remaining time before timeout in milliseconds.
     *
     * @return the remaining time, or -1 if no timeout
     */
    public long getRemainingTime() {
        if (currentNode == null) {
            return -1;
        }
        long timeout = currentNode.getTimeoutMillis(context);
        if (timeout <= 0) {
            return -1;
        }
        long elapsed = System.currentTimeMillis() - lastInteractionTime;
        return Math.max(0, timeout - elapsed);
    }
}
