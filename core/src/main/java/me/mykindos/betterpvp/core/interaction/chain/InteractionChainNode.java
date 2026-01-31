package me.mykindos.betterpvp.core.interaction.chain;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.RootInteraction;
import me.mykindos.betterpvp.core.interaction.followup.InteractionFollowUps;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
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
 * Root nodes store a {@link RootInteraction} which provides the display name and description
 * for the chain. Non-root nodes store only an {@link Interaction}.
 */
@Getter
public class InteractionChainNode {

    private final InteractionInput triggerInput;
    private final Interaction interaction;
    private final RootInteraction rootInteraction;
    private final long timeoutMillis;
    private final int requiredInputCount;
    private final List<InteractionChainNode> children;
    private InteractionChainNode parent;

    @Setter
    private InteractionFollowUps followUps;

    /**
     * Create a new root chain node with display metadata.
     *
     * @param triggerInput       the input that triggers this node
     * @param rootInteraction    the root interaction containing name, description, and interaction
     * @param timeoutMillis      the timeout in milliseconds before the chain resets
     * @param requiredInputCount the number of inputs required to trigger (e.g., 3 for triple-click)
     */
    public InteractionChainNode(@NotNull InteractionInput triggerInput, @NotNull RootInteraction rootInteraction,
                                 long timeoutMillis, int requiredInputCount) {
        this.triggerInput = triggerInput;
        this.rootInteraction = rootInteraction;
        this.interaction = rootInteraction.interaction();
        this.timeoutMillis = timeoutMillis;
        this.requiredInputCount = requiredInputCount;
        this.children = new ArrayList<>();
    }

    /**
     * Create a new non-root chain node.
     *
     * @param triggerInput       the input that triggers this node
     * @param interaction        the interaction to execute
     * @param timeoutMillis      the timeout in milliseconds before the chain resets
     * @param requiredInputCount the number of inputs required to trigger (e.g., 3 for triple-click)
     */
    public InteractionChainNode(@NotNull InteractionInput triggerInput, @NotNull Interaction interaction,
                                 long timeoutMillis, int requiredInputCount) {
        this.triggerInput = triggerInput;
        this.interaction = interaction;
        this.rootInteraction = null;
        this.timeoutMillis = timeoutMillis;
        this.requiredInputCount = requiredInputCount;
        this.children = new ArrayList<>();
    }

    /**
     * Create a new non-root chain node with single input requirement.
     *
     * @param triggerInput  the input that triggers this node
     * @param interaction   the interaction to execute
     * @param timeoutMillis the timeout in milliseconds
     */
    public InteractionChainNode(@NotNull InteractionInput triggerInput, @NotNull Interaction interaction,
                                 long timeoutMillis) {
        this(triggerInput, interaction, timeoutMillis, 1);
    }

    /**
     * Create a root node with no timeout.
     *
     * @param triggerInput    the input that triggers this node
     * @param rootInteraction the root interaction containing name, description, and interaction
     */
    public InteractionChainNode(@NotNull InteractionInput triggerInput, @NotNull RootInteraction rootInteraction) {
        this(triggerInput, rootInteraction, 0, 1);
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
     *
     * @param input the input to match
     * @return the matching child, if any
     */
    public Optional<InteractionChainNode> findChild(@NotNull InteractionInput input) {
        return children.stream()
                .filter(child -> child.getTriggerInput().equals(input))
                .findFirst();
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
     * Check if this is a root node (has a RootInteraction).
     *
     * @return true if this is a root node
     */
    public boolean isRoot() {
        return rootInteraction != null;
    }

    /**
     * Get the display name for this root node.
     *
     * @return the display name, or null if this is not a root node
     */
    @Nullable
    public String getDisplayName() {
        return rootInteraction != null ? rootInteraction.name() : null;
    }

    /**
     * Get the description for this root node.
     *
     * @return the description, or null if this is not a root node
     */
    @Nullable
    public String getDescription() {
        return rootInteraction != null ? rootInteraction.description() : null;
    }

    /**
     * Check if this node requires multiple inputs to trigger.
     *
     * @return true if multiple inputs are required
     */
    public boolean requiresMultipleInputs() {
        return requiredInputCount > 1;
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
