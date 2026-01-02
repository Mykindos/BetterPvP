package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("core:bow")
@FallbackItem(value = Material.BOW, keepRecipes = true)
public class Bow extends VanillaItem {

    public Bow() {
        super("Bow", ItemStack.of(Material.BOW), ItemRarity.COMMON);
        addSerializableComponent(new RuneContainerComponent(0, 0));
    }
}
