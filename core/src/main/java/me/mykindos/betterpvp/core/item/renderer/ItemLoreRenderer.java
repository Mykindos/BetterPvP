package me.mykindos.betterpvp.core.item.renderer;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import me.mykindos.betterpvp.core.item.ItemInstance;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Renderer for displaying lore on items.
 *
 * <p>Implementations produce lore lines as authored, possibly <b>unresolved</b> translatable components.
 * Server-side localization into a recipient's locale happens later, at the outgoing packet boundary (see
 * {@code ItemPacketRemapper} / {@link me.mykindos.betterpvp.core.locale.Translations#renderItemStack}).
 * Do not resolve translations here.</p>
 */
@SuppressWarnings("ALL")
public interface ItemLoreRenderer {

    /**
     * Create the lore lines for a specific {@link LorePages page} of an item.
     * @param item The item to create lore for
     * @param itemStack The item stack being rendered
     * @param page The page to render
     * @return the lore lines for that page
     */
    List<Component> create(ItemInstance item, ItemStack itemStack, int page);

    /**
     * Create lore for the item's most relevant page.
     * @param item The item to create lore for
     * @param itemStack The item stack being rendered
     * @return the lore lines for the most relevant page
     */
    default List<Component> create(ItemInstance item, ItemStack itemStack) {
        return create(item, itemStack, LorePages.mostRelevant(item));
    }

    /**
     * Render the lore for a specific page of an item.
     * @param item The item to render lore for
     * @param itemStack The item stack to render lore on
     * @param page The page to render
     */
    default void write(ItemInstance item, ItemStack itemStack, int page) {
        final ItemLore lore = ItemLore.lore(create(item, itemStack, page));
        itemStack.setData(DataComponentTypes.LORE, lore);
    }

    /**
     * Render the lore for an item's most relevant page.
     * @param item The item to render lore for
     * @param itemStack The item stack to render lore on
     */
    default void write(ItemInstance item, ItemStack itemStack) {
        write(item, itemStack, LorePages.mostRelevant(item));
    }

    /**
     * Clear the lore for an item.
     */
    default void clear(ItemInstance item, ItemStack itemStack) {
        final ItemLore lore = ItemLore.lore(List.of());
        itemStack.setData(DataComponentTypes.LORE, lore);
    }

}
