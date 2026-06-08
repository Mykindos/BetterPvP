package me.mykindos.betterpvp.core.item.component.impl.socketables;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.inventory.ItemStack;

/**
 * Base class for all rune items in the system.
 * Runes are special items that can be applied to equipment to enhance their properties.
 */
@Getter
public abstract class SocketableItem extends BaseItem {

    protected final Socketable socketable;

    /**
     * Creates a new rune with the specified properties
     *
     * @param model  The item model to use
     * @param rarity The rarity of this rune
     */
    protected SocketableItem(Socketable socketable, ItemStack itemStack, ItemGroup itemGroup, ItemRarity rarity) {
        super(socketable.getDisplayName(), itemStack, itemGroup, rarity);
        this.socketable = socketable;
        addBaseComponent(new SocketableDescriptionComponent(socketable));
    }

}
