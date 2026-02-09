package me.mykindos.betterpvp.core.interaction.chain;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.interaction.AbilityDisplay;
import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.followup.InteractionFollowUps;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.interaction.timing.InputCount;
import me.mykindos.betterpvp.core.interaction.timing.Timing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A node in an interaction chain tree.
 * Each node contains an interaction and can have children triggered by different inputs.
 * <p>
 * Nodes can optionally have display info ({@link AbilityDisplay}) for rendering in item lore.
 * Hidden nodes (no display info) are not shown in lore but still execute normally.
 */
@Getter
public class InteractionChainNode {

    private static long nodeIdCounter = 0;

    private final long id = ++nodeIdCounter;
    private final InteractionInput triggerInput;
    private final Interaction interaction;
    private final @Nullable AbilityDisplay displayInfo;
    private final Timing timeout;
    private final Timing minimumDelay;
    private final InputCount requiredInputCount;
    private final List<InteractionChainNode> children;
    private InteractionChainNode parent;

    @Setter
    private InteractionFollowUps followUps;

    /**
     * Create a node with full timing control.
     *
     * @param triggerInput       the input that triggers this node
     * @param interaction        the interaction to execute
     * @param displayInfo        the display info for lore (null for hidden nodes)
     * @param timeout            the timeout before the chain resets
     * @param minimumDelay       the minimum delay before this node can be triggered
     * @param requiredInputCount the number of inputs required to trigger
     */
    public InteractionChainNode(@NotNull InteractionInput triggerInput,
                                 @NotNull Interaction interaction,
                                 @Nullable AbilityDisplay displayInfo,
                                 @NotNull Timing timeout,
                                 @NotNull Timing minimumDelay,
                                 @NotNull InputCount requiredInputCount) {
        this.triggerInput = triggerInput;
        this.interaction = interaction;
        this.displayInfo = displayInfo;
        this.timeout = timeout;
        this.minimumDelay = minimumDelay;
        this.requiredInputCount = requiredInputCount;
        this.children = new ArrayList<>();
    }

    /**
     * Create a node with timeout only (no delay, single input).
     *
     * @param triggerInput the input that triggers this node
     * @param interaction  the interaction to execute
     * @param displayInfo  the display info for lore (null for hidden nodes)
     * @param timeout      the timeout before the chain resets
     */
    public InteractionChainNode(@NotNull InteractionInput triggerInput,
                                 @NotNull Interaction interaction,
                                 @Nullable AbilityDisplay displayInfo,
                                 @NotNull Timing timeout) {
        this(triggerInput, interaction, displayInfo, timeout, Timing.ZERO, InputCount.ONE);
    }

    /**
     * Create a hidden node with full timing control.
     *
     * @param triggerInput       the input that triggers this node
     * @param interaction        the interaction to execute
     * @param timeout            the timeout before the chain resets
     * @param minimumDelay       the minimum delay before this node can be triggered
     * @param requiredInputCount the number of inputs required to trigger
     */
    public InteractionChainNode(@NotNull InteractionInput triggerInput,
                                 @NotNull Interaction interaction,
                                 @NotNull Timing timeout,
                                 @NotNull Timing minimumDelay,
                                 @NotNull InputCount requiredInputCount) {
        this(triggerInput, interaction, null, timeout, minimumDelay, requiredInputCount);
    }

    /**
     * Create a hidden node with timeout only (no delay, single input).
     *
     * @param triggerInput the input that triggers this node
     * @param interaction  the interaction to execute
     * @param timeout      the timeout before the chain resets
     */
    public InteractionChainNode(@NotNull InteractionInput triggerInput,
                                 @NotNull Interaction interaction,
                                 @NotNull Timing timeout) {
        this(triggerInput, interaction, null, timeout, Timing.ZERO, InputCount.ONE);
    }

    /**
     * Get the current timeout in milliseconds.
     *
     * @param context the interaction context (may be null)
     * @return the timeout in milliseconds
     */
    public long getTimeoutMillis(@Nullable InteractionContext context) {
        return timeout.getMillis(context);
    }

    /**
     * Get the current minimum delay in milliseconds.
     *
     * @param context the interaction context (may be null)
     * @return the minimum delay in milliseconds
     */
    public long getMinimumDelayMillis(@Nullable InteractionContext context) {
        return minimumDelay.getMillis(context);
    }

    /**
     * Get the current required input count.
     *
     * @param context the interaction context (may be null)
     * @return the number of inputs required to trigger
     */
    public int getRequiredInputCount(@Nullable InteractionContext context) {
        return requiredInputCount.get(context);
    }

    /**
     * Add a child node to this node.
     *
     * @param child the child node
     */
    public void addChild(@NotNull InteractionChainNode child) {
        child.parent = this;
        children.add(child);
    }

    /**
     * Get an unmodifiable view of the children.
     *
     * @return the children
     */
    public List<InteractionChainNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Find a child node that matches the given input.
     * <p>
     * If the input is SHIFT_LEFT_CLICK or SHIFT_RIGHT_CLICK and no child exists for that input,
     * this method will fall back to LEFT_CLICK or RIGHT_CLICK respectively for QoL.
     *
     * @param input the input to match
     * @return the matching child, if any
     */
    public Optional<InteractionChainNode> findChild(@NotNull InteractionInput input) {
        Optional<InteractionChainNode> child = children.stream()
                .filter(c -> c.getTriggerInput().equals(input))
                .findFirst();

        if (child.isPresent()) {
            return child;
        }

        // Fallback: SHIFT variants fall back to non-SHIFT if not explicitly defined
        if (input == InteractionInputs.SHIFT_LEFT_CLICK) {
            return children.stream()
                    .filter(c -> c.getTriggerInput().equals(InteractionInputs.LEFT_CLICK))
                    .findFirst();
        } else if (input == InteractionInputs.SHIFT_RIGHT_CLICK) {
            return children.stream()
                    .filter(c -> c.getTriggerInput().equals(InteractionInputs.RIGHT_CLICK))
                    .findFirst();
        }

        return Optional.empty();
    }

    /**
     * Check if this node has children.
     *
     * @return true if this node has children
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Check if this is a root node (has no parent).
     *
     * @return true if this is a root node
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Check if this node should be displayed in lore.
     *
     * @return true if this node has display info
     */
    public boolean hasDisplay() {
        return displayInfo != null;
    }

    /**
     * Check if this node requires multiple inputs to trigger.
     *
     * @param context the interaction context (may be null)
     * @return true if multiple inputs are required
     */
    public boolean requiresMultipleInputs(@Nullable InteractionContext context) {
        return requiredInputCount.requiresMultiple(context);
    }

    /**
     * Check if this node has a minimum delay requirement.
     *
     * @param context the interaction context (may be null)
     * @return true if a minimum delay is required
     */
    public boolean hasMinimumDelay(@Nullable InteractionContext context) {
        return !minimumDelay.isZero() && minimumDelay.getMillis(context) > 0;
    }

    /**
     * Get the parent node.
     *
     * @return the parent, or null if this is a root
     */
    @Nullable
    public InteractionChainNode getParent() {
        return parent;
    }

    /**
     * Check if this node has follow-up interactions defined.
     *
     * @return true if follow-ups are defined
     */
    public boolean hasFollowUps() {
        return followUps != null && followUps.hasFollowUps();
    }
}
