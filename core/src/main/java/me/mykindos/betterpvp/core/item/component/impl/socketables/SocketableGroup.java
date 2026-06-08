package me.mykindos.betterpvp.core.item.component.impl.socketables;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

/**
 * Represents a group of runes that can be applied to a specific class of items.
 * A {@link SocketableGroup} holds a localized display name and a predicate that determines
 * whether a given {@link Item} belongs to the group.
 */
@AllArgsConstructor
public class SocketableGroup {

    /**
     * Translation key for the name displayed for this rune group.
     */
    private final String translationKey;

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

    public Component getDisplayComponent() {
        return Translations.component(translationKey);
    }

    public boolean canApply(Item item) {
        return itemPredicate.test(item);
    }

}
