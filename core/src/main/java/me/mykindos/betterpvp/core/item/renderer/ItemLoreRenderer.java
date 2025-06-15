package me.mykindos.betterpvp.core.item.renderer;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import me.mykindos.betterpvp.core.item.ItemInstance;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Renderer for displaying lore on items.
 */
@SuppressWarnings("ALL")
public interface ItemLoreRenderer {

    /**
     * Create a list of components to be used as lore for an item.
     * @param item The item to create lore for
     * @param itemStack
     * @return
     */
    List<Component> create(ItemInstance item, ItemStack itemStack);

    /**
     * Render the lore for an item.
     * @param item The item to render lore for
     * @param itemStack The item stack to render lore on
     */
    default void write(ItemInstance item, ItemStack itemStack) {
        final ItemLore lore = ItemLore.lore(create(item, itemStack));
        itemStack.setData(DataComponentTypes.LORE, lore);
    }

    /**
     * Clear the lore for an item.
     */
    default void clear(ItemInstance item, ItemStack itemStack) {
        final ItemLore lore = ItemLore.lore(List.of());
        itemStack.setData(DataComponentTypes.LORE, lore);
    }

}
