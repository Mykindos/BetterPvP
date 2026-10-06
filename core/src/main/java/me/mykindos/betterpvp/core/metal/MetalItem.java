package me.mykindos.betterpvp.core.metal;

import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/**
 * Represents a metal ingot item in the game.
 * This class serves as a base for all metal ingots, providing common properties and methods.
 */
public abstract class MetalItem extends BaseItem {

    /**
     * @param model the item model under betterpvp:item/metal/
     */
    protected MetalItem(Component name, String model, ItemRarity rarity) {
        super(name, Item.builder(Material.PAPER).model("item/metal/" + model).build(), ItemGroup.MATERIAL, rarity);
    }

}
