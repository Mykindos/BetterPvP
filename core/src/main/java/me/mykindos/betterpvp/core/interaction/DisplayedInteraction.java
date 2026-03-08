package me.mykindos.betterpvp.core.interaction;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Marker interface for self-contained interactions that define their own display metadata.
 * <p>
 * Implement this for abilities that always display with the same name and description,
 * like "Skyforged Ascent" on Mjolnir.
 * <p>
 * For generic/reusable interactions (VFX, Dash, etc.), do NOT implement this -
 * instead specify display at the usage site via the builder.
 * <p>
 * Components support MiniMessage tags and custom formatting. A fallback style
 * is applied when rendering in lore (yellow for name, white for description).
 */
public interface DisplayedInteraction extends Interaction {

    /**
     * Get the user-facing display name for this ability.
     * Shown in item lore. Supports MiniMessage formatting.
     */
    @NotNull
    Component getDisplayName();

    /**
     * Get the user-facing description for this ability.
     * Shown in item lore. Supports MiniMessage formatting.
     */
    @NotNull
    Component getDisplayDescription();
}
