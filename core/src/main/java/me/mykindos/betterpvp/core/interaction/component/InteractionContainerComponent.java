package me.mykindos.betterpvp.core.interaction.component;

import com.google.common.base.Preconditions;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.interaction.AbilityDisplay;
import me.mykindos.betterpvp.core.interaction.DisplayedInteraction;
import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChain;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.followup.InteractionFollowUps;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
import me.mykindos.betterpvp.core.interaction.timing.InputCount;
import me.mykindos.betterpvp.core.interaction.timing.Timing;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.utilities.ComponentWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
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

        // Filter to only displayed roots
        List<InteractionChainNode> displayedRoots = chain.getRoots().stream()
                .filter(InteractionChainNode::hasDisplay)
                .sorted(Comparator.comparing(a -> a.getTriggerInput().getName()))
                .toList();

        for (int i = 0; i < displayedRoots.size(); i++) {
            InteractionChainNode root = displayedRoots.get(i);
            AbilityDisplay display = root.getDisplayInfo();

            // Apply fallback style (white) to description - custom colors override
            Component descriptionWithFallback = display.description()
                    .applyFallbackStyle(Style.style(NamedTextColor.WHITE));
            List<Component> components = ComponentWrapper.wrapLine(descriptionWithFallback, 30, true);

            // Apply fallback style (yellow) to name - custom colors override
            Component nameWithFallback = display.name()
                    .applyFallbackStyle(Style.style(NamedTextColor.YELLOW));

            Component title = nameWithFallback
                    .appendSpace()
                    .append(root.getTriggerInput().getDisplayName().applyFallbackStyle(
                            Style.style(NamedTextColor.YELLOW, TextDecoration.BOLD)));
            components.addFirst(title);

            if (i < displayedRoots.size() - 1) {
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

        // Track pending follow-ups for current node
        private final List<Interaction> pendingOnComplete = new ArrayList<>();
        private final List<Interaction> pendingOnFail = new ArrayList<>();
        private final List<Interaction> pendingAlways = new ArrayList<>();

        private Builder() {
            this.chain = new InteractionChain();
        }

        /**
         * Add a self-contained displayed root with no timeout.
         *
         * @param input       the trigger input
         * @param interaction the displayed interaction
         * @return this builder
         */
        public Builder root(@NotNull InteractionInput input, @NotNull DisplayedInteraction interaction) {
            return root(input, interaction, Timing.ZERO);
        }

        /**
         * Add a self-contained displayed root with timeout.
         *
         * @param input       the trigger input
         * @param interaction the displayed interaction
         * @param timeout     the timeout before the chain resets
         * @return this builder
         */
        public Builder root(@NotNull InteractionInput input, @NotNull DisplayedInteraction interaction,
                            @NotNull Timing timeout) {
            return root(input, interaction, timeout, Timing.ZERO, InputCount.ONE);
        }

        /**
         * Add a self-contained displayed root with timeout and delay.
         *
         * @param input        the trigger input
         * @param interaction  the displayed interaction
         * @param timeout      the timeout before the chain resets
         * @param minimumDelay the minimum time that must pass since the last chain completion
         * @return this builder
         */
        public Builder root(@NotNull InteractionInput input, @NotNull DisplayedInteraction interaction,
                            @NotNull Timing timeout, @NotNull Timing minimumDelay) {
            return root(input, interaction, timeout, minimumDelay, InputCount.ONE);
        }

        /**
         * Add a self-contained displayed root with full timing control.
         *
         * @param input        the trigger input
         * @param interaction  the displayed interaction
         * @param timeout      the timeout before the chain resets
         * @param minimumDelay the minimum time that must pass since the last chain completion
         * @param inputCount   the number of inputs required to trigger
         * @return this builder
         */
        public Builder root(@NotNull InteractionInput input, @NotNull DisplayedInteraction interaction,
                            @NotNull Timing timeout, @NotNull Timing minimumDelay, @NotNull InputCount inputCount) {
            AbilityDisplay display = AbilityDisplay.from(interaction);
            InteractionChainNode node = new InteractionChainNode(
                    input, interaction, display, timeout, minimumDelay, inputCount
            );
            chain.addRoot(node);
            currentNode = node;
            return this;
        }

        /**
         * Add a generic interaction with explicit display (Component).
         * Supports MiniMessage formatting and custom colors.
         *
         * @param input       the trigger input
         * @param displayName the display name for lore
         * @param description the description for lore
         * @param interaction the interaction to execute
         * @return this builder
         */
        public Builder root(@NotNull InteractionInput input, @NotNull Component displayName,
                            @NotNull Component description, @NotNull Interaction interaction) {
            return root(input, displayName, description, interaction, Timing.ZERO);
        }

        /**
         * Add a generic interaction with explicit display (Component) and timeout.
         *
         * @param input       the trigger input
         * @param displayName the display name for lore
         * @param description the description for lore
         * @param interaction the interaction to execute
         * @param timeout     the timeout before the chain resets
         * @return this builder
         */
        public Builder root(@NotNull InteractionInput input, @NotNull Component displayName,
                            @NotNull Component description, @NotNull Interaction interaction,
                            @NotNull Timing timeout) {
            return root(input, displayName, description, interaction, timeout, Timing.ZERO, InputCount.ONE);
        }

        /**
         * Add a generic interaction with explicit display (Component), timeout, and delay.
         *
         * @param input        the trigger input
         * @param displayName  the display name for lore
         * @param description  the description for lore
         * @param interaction  the interaction to execute
         * @param timeout      the timeout before the chain resets
         * @param minimumDelay the minimum time that must pass since the last chain completion
         * @param inputCount   the number of inputs required to trigger
         * @return this builder
         */
        public Builder root(@NotNull InteractionInput input, @NotNull Component displayName,
                            @NotNull Component description, @NotNull Interaction interaction,
                            @NotNull Timing timeout, @NotNull Timing minimumDelay, @NotNull InputCount inputCount) {
            AbilityDisplay display = AbilityDisplay.of(displayName, description);
            InteractionChainNode node = new InteractionChainNode(
                    input, interaction, display, timeout, minimumDelay, inputCount
            );
            chain.addRoot(node);
            currentNode = node;
            return this;
        }

        /**
         * Add a generic interaction with explicit display (String convenience).
         * Wraps strings in Component.text().
         *
         * @param input       the trigger input
         * @param displayName the display name for lore
         * @param description the description for lore
         * @param interaction the interaction to execute
         * @return this builder
         */
        public Builder root(@NotNull InteractionInput input, @NotNull String displayName,
                            @NotNull String description, @NotNull Interaction interaction) {
            return root(input, Component.text(displayName), Component.text(description), interaction);
        }

        /**
         * Add a generic interaction with explicit display (String convenience) and timeout.
         *
         * @param input       the trigger input
         * @param displayName the display name for lore
         * @param description the description for lore
         * @param interaction the interaction to execute
         * @param timeout     the timeout before the chain resets
         * @return this builder
         */
        public Builder root(@NotNull InteractionInput input, @NotNull String displayName,
                            @NotNull String description, @NotNull Interaction interaction,
                            @NotNull Timing timeout) {
            return root(input, Component.text(displayName), Component.text(description), interaction, timeout);
        }

        /**
         * Add a hidden root - will NOT appear in item lore.
         * Use for internal triggers like DAMAGE_DEALT, passive effects, etc.
         *
         * @param input       the trigger input
         * @param interaction the interaction to execute
         * @return this builder
         */
        public Builder hiddenRoot(@NotNull InteractionInput input, @NotNull Interaction interaction) {
            return hiddenRoot(input, interaction, Timing.ZERO);
        }

        /**
         * Add a hidden root with timeout.
         *
         * @param input       the trigger input
         * @param interaction the interaction to execute
         * @param timeout     the timeout before the chain resets
         * @return this builder
         */
        public Builder hiddenRoot(@NotNull InteractionInput input, @NotNull Interaction interaction,
                                   @NotNull Timing timeout) {
            return hiddenRoot(input, interaction, timeout, Timing.ZERO, InputCount.ONE);
        }

        /**
         * Add a hidden root with timeout and delay.
         *
         * @param input        the trigger input
         * @param interaction  the interaction to execute
         * @param timeout      the timeout before the chain resets
         * @param minimumDelay the minimum time that must pass since the last chain completion
         * @return this builder
         */
        public Builder hiddenRoot(@NotNull InteractionInput input, @NotNull Interaction interaction,
                                   @NotNull Timing timeout, @NotNull Timing minimumDelay) {
            return hiddenRoot(input, interaction, timeout, minimumDelay, InputCount.ONE);
        }

        /**
         * Add a hidden root with full timing control.
         *
         * @param input        the trigger input
         * @param interaction  the interaction to execute
         * @param timeout      the timeout before the chain resets
         * @param minimumDelay the minimum time that must pass since the last chain completion
         * @param inputCount   the number of inputs required to trigger
         * @return this builder
         */
        public Builder hiddenRoot(@NotNull InteractionInput input, @NotNull Interaction interaction,
                                   @NotNull Timing timeout, @NotNull Timing minimumDelay, @NotNull InputCount inputCount) {
            InteractionChainNode node = new InteractionChainNode(
                    input, interaction, timeout, minimumDelay, inputCount
            );
            chain.addRoot(node);
            currentNode = node;
            return this;
        }

        /**
         * Add a child interaction to the current node.
         * Chain nodes are never displayed in lore - they're internal progression.
         *
         * @param input       the trigger input
         * @param timeout     the timeout before the chain resets
         * @param interaction the interaction to execute
         * @return this builder
         */
        public Builder chain(@NotNull InteractionInput input, @NotNull Timing timeout,
                             @NotNull Interaction interaction) {
            return chain(input, timeout, Timing.ZERO, InputCount.ONE, interaction);
        }

        /**
         * Add a child interaction with minimum delay.
         *
         * @param input        the trigger input
         * @param timeout      the timeout before the chain resets
         * @param minimumDelay the minimum time that must pass since the last interaction
         * @param interaction  the interaction to execute
         * @return this builder
         */
        public Builder chain(@NotNull InteractionInput input, @NotNull Timing timeout,
                             @NotNull Timing minimumDelay, @NotNull Interaction interaction) {
            return chain(input, timeout, minimumDelay, InputCount.ONE, interaction);
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
        public Builder chain(@NotNull InteractionInput input, @NotNull Timing timeout,
                             @NotNull InputCount inputCount, @NotNull Interaction interaction) {
            return chain(input, timeout, Timing.ZERO, inputCount, interaction);
        }

        /**
         * Add a child interaction with full timing control.
         *
         * @param input        the trigger input
         * @param timeout      the timeout before the chain resets
         * @param minimumDelay the minimum time that must pass since the last interaction
         * @param inputCount   the number of inputs required to trigger
         * @param interaction  the interaction to execute
         * @return this builder
         */
        public Builder chain(@NotNull InteractionInput input, @NotNull Timing timeout,
                             @NotNull Timing minimumDelay, @NotNull InputCount inputCount,
                             @NotNull Interaction interaction) {
            Preconditions.checkState(currentNode != null, "No current node to chain from. Call root() first.");

            InteractionChainNode child = new InteractionChainNode(
                    input, interaction, timeout, minimumDelay, inputCount
            );
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
         * Add follow-up interaction(s) to execute when current node completes successfully.
         *
         * @param interactions the interactions to execute on success
         * @return this builder
         */
        public Builder onComplete(@NotNull Interaction... interactions) {
            Preconditions.checkState(currentNode != null, "No current node. Call root() or chain() first.");
            Collections.addAll(pendingOnComplete, interactions);
            flushFollowUps();
            return this;
        }

        /**
         * Add follow-up interaction(s) to execute when current node fails.
         *
         * @param interactions the interactions to execute on failure
         * @return this builder
         */
        public Builder onFail(@NotNull Interaction... interactions) {
            Preconditions.checkState(currentNode != null, "No current node. Call root() or chain() first.");
            Collections.addAll(pendingOnFail, interactions);
            flushFollowUps();
            return this;
        }

        /**
         * Add follow-up interaction(s) to execute regardless of current node's outcome.
         *
         * @param interactions the interactions to always execute
         * @return this builder
         */
        public Builder then(@NotNull Interaction... interactions) {
            Preconditions.checkState(currentNode != null, "No current node. Call root() or chain() first.");
            Collections.addAll(pendingAlways, interactions);
            flushFollowUps();
            return this;
        }

        /**
         * Flush accumulated follow-ups to the current node.
         */
        private void flushFollowUps() {
            if (pendingOnComplete.isEmpty() && pendingOnFail.isEmpty() && pendingAlways.isEmpty()) {
                return;
            }

            // Get existing follow-ups or create new
            InteractionFollowUps existing = currentNode.getFollowUps();
            InteractionFollowUps.Builder builder = InteractionFollowUps.builder();

            // Add existing
            if (existing != null) {
                existing.getOnComplete().forEach(builder::onComplete);
                existing.getOnFail().forEach(builder::onFail);
                existing.getAlways().forEach(builder::then);
            }

            // Add new
            pendingOnComplete.forEach(builder::onComplete);
            pendingOnFail.forEach(builder::onFail);
            pendingAlways.forEach(builder::then);

            // Clear pending
            pendingOnComplete.clear();
            pendingOnFail.clear();
            pendingAlways.clear();

            // Set on node
            currentNode.setFollowUps(builder.build());
        }

        /**
         * Set follow-up interactions for the current node.
         * Follow-ups are executed after the current node's interaction completes.
         *
         * @param followUps the follow-up interactions to attach
         * @return this builder
         * @deprecated Use {@link #onComplete}, {@link #onFail}, or {@link #then} instead.
         */
        @Deprecated(forRemoval = true)
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
