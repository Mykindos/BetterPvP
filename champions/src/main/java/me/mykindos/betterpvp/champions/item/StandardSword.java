package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
public class StandardSword extends WeaponItem {

    private static final ItemStack model;

    static {
        model = ItemStack.of(Material.IRON_SWORD);
        model.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(Float.MAX_VALUE)
                .animation(ItemUseAnimation.BLOCK)
                .build());
    }

    @Inject
    private StandardSword(Champions champions) {
        super(champions, "Standard Sword", model, ItemRarity.COMMON);
    }

}
