package me.mykindos.betterpvp.core.item.component.impl.runes;

import me.mykindos.betterpvp.core.item.Item;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a rune that can be applied to items.
 * Runes are special enhancements that can modify the properties of items.
 *
 * @see RuneItem
 */
public interface Rune {

    /**
     * Gets the namespaced key that is used when this rune is applied to an item.
     *
     * @return The namespaced key
     */
    @NotNull NamespacedKey getKey();

    /**
     * Gets the name of this rune.
     *
     * @return The name of the rune
     */
    @NotNull String getName();

    /**
     * Gets the description of this rune.
     *
     * @return The description of the rune
     */
    @NotNull String getDescription();

    /**
     * Checks if this rune can be applied to the specified item.
     *
     * @param item The item instance to check
     * @return true if the rune can be applied
     */
    boolean canApply(@NotNull Item item);

}
