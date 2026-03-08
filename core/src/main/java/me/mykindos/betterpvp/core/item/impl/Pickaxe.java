package me.mykindos.betterpvp.core.item.impl;

import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Pickaxe extends VanillaItem {

    public Pickaxe(String name, Material material, ItemRarity rarity) {
        super(name, ItemStack.of(material), rarity);
        addSerializableComponent(new RuneContainerComponent(0, 0));
    }
}
