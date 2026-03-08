package me.mykindos.betterpvp.clans.item;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.FallbackItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("clans:water_block")
@FallbackItem(Material.LAPIS_LAZULI)
public class WaterBlock extends BaseItem {

    public WaterBlock() {
        super("Water Block", ItemStack.of(Material.LAPIS_BLOCK), ItemGroup.BLOCK, ItemRarity.COMMON);
    }
}
