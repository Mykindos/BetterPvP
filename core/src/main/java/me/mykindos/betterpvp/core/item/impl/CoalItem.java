package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.fuel.FuelComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("core:coal_item")
@FallbackItem(value = Material.COAL, keepRecipes = true)
public class CoalItem extends BaseItem {

    private static final ItemStack model;

    static {
        model = ItemStack.of(Material.COAL);
    }

    @Inject
    private CoalItem() {
        super("Coal", model, ItemGroup.MATERIAL, ItemRarity.COMMON);
        addBaseComponent(new FuelComponent(10_000L, 800.0f));
    }
} 