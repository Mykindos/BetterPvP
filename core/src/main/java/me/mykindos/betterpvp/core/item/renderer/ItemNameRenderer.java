package me.mykindos.betterpvp.core.item.renderer;

import me.mykindos.betterpvp.core.item.ItemInstance;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

/**
 * Renderer for display names on items.
 */
@FunctionalInterface
public interface ItemNameRenderer {

    /**
     * Create the name for an item.
     * @param item The item to create the name for
     * @param itemStack The item stack to apply the name to
     * @return The component representing the name of the item
     */
    Component createName(ItemInstance item);

    /**
     * Render the name for an item.
     * @param item The item to render the name for
     * @param itemStack The item stack to apply the name to
     */
    default void write(ItemInstance item, ItemStack itemStack) {
        itemStack.getItemMeta().displayName(createName(item));
    }

    /**
     * Clear the name for an item.
     */
    default void clear(ItemInstance item, ItemStack itemStack) {
        itemStack.getItemMeta().displayName(null);
    }

}
