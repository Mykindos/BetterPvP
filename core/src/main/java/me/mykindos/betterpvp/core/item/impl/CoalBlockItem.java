package me.mykindos.betterpvp.core.item.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.fuel.FuelComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
public class CoalBlockItem extends BaseItem {

    private static final ItemStack model;

    static {
        model = ItemStack.of(Material.COAL);
    }

    @Inject
    private CoalBlockItem() {
        super("Coal Block", model, ItemGroup.MATERIAL, ItemRarity.COMMON);
        addBaseComponent(new FuelComponent(90_000L, 800.0f));
    }
} 