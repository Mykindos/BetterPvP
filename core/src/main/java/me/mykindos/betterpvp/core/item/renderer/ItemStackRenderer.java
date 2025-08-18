package me.mykindos.betterpvp.core.item.renderer;

import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.inventory.ItemStack;

/**
 * At the end of the rendering process in {@link me.mykindos.betterpvp.core.item.ItemInstanceView},
 * this is called to edit the item that the player sees.
 */
@FunctionalInterface
public interface ItemStackRenderer {

    /**
     * Applies this renderer to the given ItemInstance and ItemStack.
     * This method is used to modify the ItemStack that is shown to the player.
     * For example, it can add durability components, enchantments, or other visual effects.
     * <br>
     * This does NOT modify the ItemStack's metadata directly, but rather what is rendered
     * to the player.
     * @param item The ItemInstance that this renderer should apply to.
     * @param itemStack The ItemStack to which this renderer should apply its effects.
     */
    void write(ItemInstance item, ItemStack itemStack);

}
