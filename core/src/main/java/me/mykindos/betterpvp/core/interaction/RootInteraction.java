package me.mykindos.betterpvp.core.interaction;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A wrapper that provides display name and description for an interaction when used as a chain root.
 * <p>
 * This separates the chain's display metadata (name shown to users, description in lore) from the
 * interaction's execution logic. The same {@link Interaction} can be used in multiple chains with
 * different names and descriptions.
 *
 * @param name        the display name shown for this chain (e.g., in item lore)
 * @param description the description of what this chain does (shown in item lore)
 * @param interaction the interaction to execute when this root is triggered
 */
public record RootInteraction(
        @NotNull String name,
        @NotNull String description,
        @NotNull Interaction interaction
) {

    public RootInteraction {
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(interaction, "Interaction cannot be null");

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (description.isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
    }

    /**
     * Create a RootInteraction with the given name, description, and interaction.
     *
     * @param name        the display name for the chain
     * @param description the description of the chain
     * @param interaction the interaction to execute
     * @return the root interaction
     */
    public static RootInteraction of(@NotNull String name, @NotNull String description, @NotNull Interaction interaction) {
        return new RootInteraction(name, description, interaction);
    }
}
