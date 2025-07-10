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
public class CharcoalItem extends BaseItem {

    private static final ItemStack model;

    static {
        model = ItemStack.of(Material.CHARCOAL);
    }

    @Inject
    private CharcoalItem() {
        super("Charcoal", model, ItemGroup.MATERIAL, ItemRarity.COMMON);
        // Coal burns for 30 seconds (30,000ms) and can reach 800°C
        addBaseComponent(new FuelComponent(5_000L, 950.0f));
    }
} 