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
     */
    public void reset() {
        this.currentNode = null;
        this.inputCounter = 0;
        this.context.clear();
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
        long timeout = currentNode.getTimeoutMillis();
        if (timeout <= 0) {
            return false;
        }
        return System.currentTimeMillis() - lastInteractionTime > timeout;
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
        long timeout = currentNode.getTimeoutMillis();
        if (timeout <= 0) {
            return -1;
        }
        long elapsed = System.currentTimeMillis() - lastInteractionTime;
        return Math.max(0, timeout - elapsed);
    }
}
