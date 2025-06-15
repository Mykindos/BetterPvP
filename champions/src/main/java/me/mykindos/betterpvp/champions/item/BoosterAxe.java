package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
public class BoosterAxe extends WeaponItem {

    @Inject
    private BoosterAxe(Champions champions) {
        super(champions, "Booster Axe", ItemStack.of(Material.GOLDEN_AXE), ItemRarity.COMMON);
    }

    // todo: add shaped recipe

}
