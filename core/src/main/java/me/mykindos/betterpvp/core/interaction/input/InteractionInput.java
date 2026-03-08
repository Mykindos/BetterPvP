package me.mykindos.betterpvp.core.interaction.input;

import net.kyori.adventure.text.Component;

/**
 * Represents an input type that can trigger an interaction.
 * Implementations define specific input types like clicks, jumps, etc.
 */
public interface InteractionInput {

    static InteractionInput of(String name) {
        return of(name, false);
    }

    static InteractionInput of(String name, boolean allowsMultipleRoots) {
        return new InteractionInput() {
            @Override
            public Component getDisplayName() {
                return Component.text(name);
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean allowsMultipleRoots() {
                return allowsMultipleRoots;
            }
        };
    }

    /**
     * Get the display name of this input type.
     *
     * @return the display name as a Component
     */
    Component getDisplayName();

    /**
     * Get a unique identifier for this input type.
     *
     * @return the unique name
     */
    String getName();

    /**
     * Whether this input type allows multiple root interactions to be registered.
     * <p>
     * For active inputs like clicks, only one root should handle the input.
     * For passive inputs like PASSIVE or DAMAGE_DEALT, multiple roots can coexist.
     *
     * @return true if multiple roots are allowed for this input
     */
    default boolean allowsMultipleRoots() {
        return false;
    }
}
