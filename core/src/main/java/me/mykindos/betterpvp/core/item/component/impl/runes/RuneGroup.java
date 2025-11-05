package me.mykindos.betterpvp.core.item.component.impl.runes;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

/**
 * Represents a group of runes that can be applied to a specific class of items.
 * A {@link RuneGroup} holds a display name and a predicate that determines
 * whether a given {@link Item} belongs to the group.
 */
@AllArgsConstructor
public class RuneGroup {

    /**
     * Name to be displayed for this rune group.
     */
    private final String displayName;

    /**
     * Predicate to test whether an item belongs to this group.
     */
    private final Predicate<Item> itemPredicate;

    static ItemStack getItemStack(Item item) {
        final ItemStack itemStack;
        if (item instanceof ItemInstance itemInstance) {
            itemStack = itemInstance.createItemStack();
        } else if (item instanceof BaseItem baseItem) {
            itemStack = baseItem.getModel();
        } else {
            throw new IllegalArgumentException("Item must be an ItemInstance or BaseItem");
        }
        return itemStack;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canApply(Item item) {
        return itemPredicate.test(item);
    }

}
