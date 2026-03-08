package me.mykindos.betterpvp.core.item.impl;

import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class Shovel extends VanillaItem {

    public Shovel(String name, Material material, ItemRarity rarity) {
        super(Component.text(name), material, rarity);
        addSerializableComponent(new RuneContainerComponent(0, 0));
    }
}
