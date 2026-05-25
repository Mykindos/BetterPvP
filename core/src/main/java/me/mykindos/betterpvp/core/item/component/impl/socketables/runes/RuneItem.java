package me.mykindos.betterpvp.core.item.component.impl.socketables.runes;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.socketables.Socketable;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableDescriptionComponent;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableItem;

/**
 * Base class for all rune items in the system.
 * Runes are special items that can be applied to equipment to enhance their properties.
 */
@Getter
public abstract class RuneItem extends SocketableItem {

    /**
     * Creates a new rune with the specified properties
     *
     * @param model  The item model to use
     * @param rarity The rarity of this rune
     */
    protected RuneItem(Socketable socketable, RuneColor color, ItemRarity rarity) {
        super(socketable, Item.builder("rune").customModelData(color.getModelData()).build(), ItemGroup.MATERIAL, rarity);
        addBaseComponent(new SocketableDescriptionComponent(socketable));
    }

}
