package me.mykindos.betterpvp.core.item.component;

import me.mykindos.betterpvp.core.item.Item;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Base interface for all item components (abilities, runes, stats, etc.).
 * Each component is responsible for its own behavior and rendering.
 * Serialization is handled by separate ComponentSerializer implementations.
 */
public interface ItemComponent extends Keyed {
    
    /**
     * Get the namespaced key used for identifying this component type.
     */
    @NotNull
    NamespacedKey getNamespacedKey();
    
    /**
     * Check if this component is compatible with the given item.
     * @param item The item to check compatibility with
     * @return True if the component can be applied to the item, false otherwise
     */
    default boolean isCompatibleWith(@NotNull Item item) {
        return true;
    }

    @Override
    default @NotNull Key key() {
        return getNamespacedKey();
    }

    /**
     * Creates a deep copy of this component.
     * @return A new instance of the component with the same properties.
     */
    ItemComponent copy();

}