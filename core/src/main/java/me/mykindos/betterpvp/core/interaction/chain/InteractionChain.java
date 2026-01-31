package me.mykindos.betterpvp.core.interaction.chain;

import lombok.Getter;
import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a complete interaction chain with one or more root entry points.
 * A chain can have multiple roots for different inputs (e.g., left-click and right-click).
 */
@Getter
public class InteractionChain {

    private static long chainIdCounter = 0;

    private final long id = ++chainIdCounter;
    private final Map<@NotNull InteractionInput, @NotNull InteractionChainNode> roots; // for O(1) lookup

    public InteractionChain() {
        this.roots = new HashMap<>();
    }

    /**
     * Add a root node to this chain.
     *
     * @param root the root node to add
     */
    public void addRoot(@NotNull InteractionChainNode root) {
        roots.put(root.getTriggerInput(), root);
    }

    /**
     * Get an unmodifiable view of the root nodes.
     *
     * @return the root nodes
     */
    public @NotNull List<@NotNull InteractionChainNode> getRoots() {
        return List.copyOf(roots.values());
    }

    /**
     * Check if this chain has a root node with the given interaction.
     * @param interaction the interaction to check for
     * @return true if this chain has a root node with the given interaction
     */
    public boolean hasRoot(@NotNull Interaction interaction) {
        return roots.values().stream().anyMatch(root -> root.getInteraction().equals(interaction));
    }

    /**
     * Find a root node that matches the given input.
     *
     * @param input the input to match
     * @return the matching root, if any
     */
    public Optional<InteractionChainNode> findRoot(@NotNull InteractionInput input) {
        return Optional.ofNullable(roots.get(input));
    }

    /**
     * Check if this chain has any roots.
     *
     * @return true if this chain has roots
     */
    public boolean hasRoots() {
        return !roots.isEmpty();
    }

    /**
     * Get all unique inputs that can trigger this chain.
     *
     * @return the list of trigger inputs
     */
    public List<InteractionInput> getTriggerInputs() {
        return roots.keySet().stream().toList();
    }
}
