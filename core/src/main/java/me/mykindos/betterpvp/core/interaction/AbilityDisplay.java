package me.mykindos.betterpvp.core.interaction;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * User-facing display information for an ability shown in item lore.
 * Used when specifying display at the builder level for generic interactions.
 * <p>
 * Components support MiniMessage tags and custom formatting. A fallback style
 * is applied when rendering in lore (yellow for name, white for description).
 */
public record AbilityDisplay(
    @NotNull Component name,
    @NotNull Component description
) {
    /**
     * Create display info from a DisplayedInteraction.
     */
    public static AbilityDisplay from(@NotNull DisplayedInteraction interaction) {
        return new AbilityDisplay(interaction.getDisplayName(), interaction.getDisplayDescription());
    }

    /**
     * Create display info with Component values (supports MiniMessage).
     */
    public static AbilityDisplay of(@NotNull Component name, @NotNull Component description) {
        return new AbilityDisplay(name, description);
    }

    /**
     * Create display info with plain String values.
     * Convenience method that wraps strings in Component.text().
     */
    public static AbilityDisplay of(@NotNull String name, @NotNull String description) {
        return new AbilityDisplay(Component.text(name), Component.text(description));
    }
}
