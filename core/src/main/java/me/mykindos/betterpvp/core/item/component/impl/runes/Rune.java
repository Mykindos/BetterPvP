package me.mykindos.betterpvp.core.item.component.impl.runes;

import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Represents a rune that can be applied to items.
 * Runes are special enhancements that can modify the properties of items.
 *
 * @see RuneItem
 */
public interface Rune {

    static boolean isDamageable(@NotNull Item item) {
        final ItemStack itemStack;
        if (item instanceof ItemInstance itemInstance) {
            itemStack = itemInstance.createItemStack();
        } else if (item instanceof BaseItem baseItem) {
            itemStack = baseItem.getModel();
        } else {
            throw new IllegalArgumentException("Item must be an ItemInstance or BaseItem");
        }
        return itemStack.hasItemMeta() && itemStack.getItemMeta() instanceof Damageable;
    }

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
     * Gets the groups that this rune belongs to.
     * @return An array of rune groups that this rune belongs to
     */
    @NotNull Collection<@NotNull RuneGroup> getGroups();

    /**
     * Checks if this rune can be applied to the specified item.
     *
     * @param item The item instance to check
     * @return true if the rune can be applied
     */
    default boolean canApply(@NotNull Item item) {
        return getGroups().stream().anyMatch(group -> group.canApply(item));
    }

}
