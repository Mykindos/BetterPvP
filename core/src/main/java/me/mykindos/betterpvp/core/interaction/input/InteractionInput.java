package me.mykindos.betterpvp.core.interaction.input;

import net.kyori.adventure.text.Component;

/**
 * Represents an input type that can trigger an interaction.
 * Implementations define specific input types like clicks, jumps, etc.
 */
public interface InteractionInput {

    static InteractionInput of(String name) {
        return new InteractionInput() {
            @Override
            public Component getDisplayName() {
                return Component.text(name);
            }

            @Override
            public String getName() {
                return name;
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
}
