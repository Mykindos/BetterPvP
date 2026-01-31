package me.mykindos.betterpvp.core.interaction.component;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.RootInteraction;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChain;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.followup.InteractionFollowUps;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Item component that contains an interaction chain.
 * Provides a fluent builder API for defining complex interaction chains.
 */
@Getter
public class InteractionContainerComponent implements ItemComponent, LoreComponent {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "interactions");
    private final InteractionChain chain;

    private InteractionContainerComponent(InteractionChain chain) {
        this.chain = chain;
    }

    /**
     * Create a new builder for an interaction container.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Find a root node matching the given input.
     *
     * @param input the input to match
     * @return the matching root, if any
     */
    public Optional<InteractionChainNode> findRoot(@NotNull InteractionInput input) {
        return chain.findRoot(input);
    }

    @Override
    public @NotNull NamespacedKey getNamespacedKey() {
        return KEY;
    }

    @Override
    public ItemComponent copy() {
        return new InteractionContainerComponent(chain);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        List<Component> lines = new ArrayList<>();
        final List<@NotNull InteractionChainNode> roots = new ArrayList<>(chain.getRoots());

        // sort by input
        roots.sort(Comparator.comparing(a -> a.getTriggerInput().getName()));

        for (int i = 0; i < chain.getRoots().size(); i++) {
            InteractionChainNode root = roots.get(i);

            // Get display name and description from the root node
            String displayName = root.getDisplayName();
            String description = root.getDescription();

            if (displayName == null || description == null) {
                continue; // Skip non-root nodes (shouldn't happen, but safety check)
            }

            Component text = Component.text(description, NamedTextColor.WHITE);
            List<Component> components = ComponentWrapper.wrapLine(text, 30, true);

            TextComponent title = Component.text(displayName, NamedTextColor.YELLOW)
                    .appendSpace()
                    .append(root.getTriggerInput().getDisplayName().applyFallbackStyle(
                            Style.style(NamedTextColor.YELLOW, TextDecoration.BOLD)));
            components.addFirst(title);

            if (i < chain.getRoots().size() - 1) {
                components.add(Component.empty());
            }

            lines.addAll(components);
        }

        return lines;
    }

    @Override
    public int getRenderPriority() {
        return -1;
    }

    /**
     * Fluent builder for creating interaction containers with chains.
     */
    public static class Builder {

        private final InteractionChain chain;
        private InteractionChainNode currentNode;

        private Builder() {
            this.chain = new InteractionChain();
        }

        /**
         * Add a root interaction triggered by the given input.
         *
         * @param input           the trigger input
         * @param rootInteraction the root interaction containing name, description, and interaction
         * @return this builder
         */
        public Builder root(@NotNull InteractionInput input, @NotNull RootInteraction rootInteraction) {
            return root(input, rootInteraction, Duration.ZERO, 1);
        }

        /**
         * Add a root interaction triggered by the given input.
         *
         * @param input           the trigger input
         * @param interaction     the root interaction containing name, description, and interaction
         * @return this builder
         */
        public Builder root(@NotNull InteractionInput input, @NotNull Interaction interaction) {
            final RootInteraction rootInteraction = new RootInteraction(interaction.getName(), interaction.getDescription(), interaction);
            return root(input, rootInteraction, Duration.ZERO, 1);
        }

        /**
         * Add a root interaction with a timeout for chaining.
         *
         * @param input           the trigger input
         * @param rootInteraction the root interaction containing name, description, and interaction
         * @param timeout         the timeout before the chain resets
         * @return this builder
         */
        public Builder root(@NotNull InteractionInput input, @NotNull RootInteraction rootInteraction, @NotNull Duration timeout) {
            return root(input, rootInteraction, timeout, 1);
        }

        /**
         * Add a root interaction requiring multiple inputs.
         *
         * @param input           the trigger input
         * @param rootInteraction the root interaction containing name, description, and interaction
         * @param timeout         the timeout before the chain resets
         * @param inputCount      the number of inputs required to trigger
         * @return this builder
         */
        public Builder root(@NotNull InteractionInput input, @NotNull RootInteraction rootInteraction,
                            @NotNull Duration timeout, int inputCount) {
            Preconditions.checkArgument(inputCount >= 1, "Input count must be at least 1");

            InteractionChainNode node = new InteractionChainNode(input, rootInteraction, timeout.toMillis(), inputCount);
            chain.addRoot(node);
            currentNode = node;
            return this;
        }

        /**
         * Add a child interaction to the current node.
         *
         * @param input       the trigger input
         * @param timeout     the timeout before the chain resets
         * @param interaction the interaction to execute
         * @return this builder
         */
        public Builder chain(@NotNull InteractionInput input, @NotNull Duration timeout,
                             @NotNull Interaction interaction) {
            return chain(input, timeout, 1, interaction);
        }

        /**
         * Add a child interaction requiring multiple inputs.
         *
         * @param input       the trigger input
         * @param timeout     the timeout before the chain resets
         * @param inputCount  the number of inputs required to trigger
         * @param interaction the interaction to execute
         * @return this builder
         */
        public Builder chain(@NotNull InteractionInput input, @NotNull Duration timeout,
                             int inputCount, @NotNull Interaction interaction) {
            Preconditions.checkState(currentNode != null, "No current node to chain from. Call root() first.");
            Preconditions.checkArgument(inputCount >= 1, "Input count must be at least 1");

            InteractionChainNode child = new InteractionChainNode(input, interaction, timeout.toMillis(), inputCount);
            currentNode.addChild(child);
            currentNode = child;
            return this;
        }

        /**
         * Navigate up one level in the chain (to add sibling branches).
         *
         * @return this builder
         */
        public Builder up() {
            Preconditions.checkState(currentNode != null, "No current node. Call root() first.");
            Preconditions.checkState(currentNode.getParent() != null, "Already at root level.");

            currentNode = currentNode.getParent();
            return this;
        }

        /**
         * Navigate back to the root level.
         *
         * @return this builder
         */
        public Builder toRoot() {
            currentNode = null;
            return this;
        }

        /**
         * Set follow-up interactions for the current node.
         * Follow-ups are executed after the current node's interaction completes.
         * <p>
         * Example usage:
         * <pre>{@code
         * .root(RIGHT_CLICK, mainInteraction)
         * .withFollowUps(InteractionFollowUps.builder()
         *     .onComplete(successInteraction)
         *     .onFail(failInteraction)
         *     .then(alwaysInteraction)
         *     .build())
         * }</pre>
         *
         * @param followUps the follow-up interactions to attach
         * @return this builder
         */
        public Builder withFollowUps(@NotNull InteractionFollowUps followUps) {
            Preconditions.checkState(currentNode != null, "No current node. Call root() or chain() first.");
            Preconditions.checkNotNull(followUps, "Follow-ups cannot be null");
            currentNode.setFollowUps(followUps);
            return this;
        }

        /**
         * Build the interaction container component.
         *
         * @return the built component
         */
        public InteractionContainerComponent build() {
            Preconditions.checkState(chain.hasRoots(), "Chain must have at least one root.");
            return new InteractionContainerComponent(chain);
        }
    }
}
