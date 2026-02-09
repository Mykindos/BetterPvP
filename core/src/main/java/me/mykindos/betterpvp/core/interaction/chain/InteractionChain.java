package me.mykindos.betterpvp.core.interaction.chain;

import lombok.Getter;
import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
    private final Map<@NotNull InteractionInput, @NotNull List<@NotNull InteractionChainNode>> roots; // for O(1) lookup

    public InteractionChain() {
        this.roots = new HashMap<>();
    }

    /**
     * Add a root node to this chain.
     *
     * @param root the root node to add
     * @throws IllegalStateException if the input doesn't allow multiple roots and one already exists
     */
    public void addRoot(@NotNull InteractionChainNode root) {
        InteractionInput input = root.getTriggerInput();
        List<InteractionChainNode> existing = roots.get(input);

        if (existing != null && !existing.isEmpty() && !input.allowsMultipleRoots()) {
            throw new IllegalStateException(
                    "Input '" + input.getName() + "' does not allow multiple roots. " +
                    "Existing root: " + existing.getFirst().getInteraction().getName() + ", " +
                    "New root: " + root.getInteraction().getName()
            );
        }

        roots.computeIfAbsent(input, k -> new ArrayList<>()).add(root);
    }

    /**
     * Get an unmodifiable view of the root nodes.
     *
     * @return the root nodes
     */
    public @NotNull List<@NotNull InteractionChainNode> getRoots() {
        return roots.values().stream().flatMap(List::stream).toList();
    }

    /**
     * Check if this chain has a root node with the given interaction.
     * @param interaction the interaction to check for
     * @return true if this chain has a root node with the given interaction
     */
    public boolean hasRoot(@NotNull Interaction interaction) {
        return roots.values().stream()
                .flatMap(List::stream)
                .anyMatch(root -> root.getInteraction().equals(interaction));
    }

    /**
     * Find the first root node that matches the given input.
     * <p>
     * If the input is SHIFT_LEFT_CLICK or SHIFT_RIGHT_CLICK and no root exists for that input,
     * this method will fall back to LEFT_CLICK or RIGHT_CLICK respectively for QoL.
     *
     * @param input the input to match
     * @return the first matching root, if any
     */
    public Optional<InteractionChainNode> findRoot(@NotNull InteractionInput input) {
        List<InteractionChainNode> nodes = findRoots(input);
        return nodes.isEmpty() ? Optional.empty() : Optional.of(nodes.getFirst());
    }

    /**
     * Find all root nodes that match the given input.
     * <p>
     * If the input is SHIFT_LEFT_CLICK or SHIFT_RIGHT_CLICK and no root exists for that input,
     * this method will fall back to LEFT_CLICK or RIGHT_CLICK respectively for QoL.
     *
     * @param input the input to match
     * @return the matching roots (may be empty)
     */
    public @NotNull List<@NotNull InteractionChainNode> findRoots(@NotNull InteractionInput input) {
        List<InteractionChainNode> nodes = roots.get(input);
        if (nodes != null && !nodes.isEmpty()) {
            return List.copyOf(nodes);
        }

        // Fallback: SHIFT variants fall back to non-SHIFT if not explicitly defined
        if (input == InteractionInputs.SHIFT_LEFT_CLICK) {
            nodes = roots.get(InteractionInputs.LEFT_CLICK);
        } else if (input == InteractionInputs.SHIFT_RIGHT_CLICK) {
            nodes = roots.get(InteractionInputs.RIGHT_CLICK);
        }

        return nodes != null ? List.copyOf(nodes) : List.of();
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
