package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("core:ancient_axe")
@FallbackItem(Material.NETHERITE_AXE)
public class AncientAxe extends WeaponItem {

    @Inject
    private AncientAxe(Champions champions) {
        super(champions, "Ancient Axe", ItemStack.of(Material.NETHERITE_AXE), ItemRarity.UNCOMMON);
    }

}
