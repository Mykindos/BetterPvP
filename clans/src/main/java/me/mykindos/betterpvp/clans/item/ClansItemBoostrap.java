package me.mykindos.betterpvp.clans.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.item.cannon.CannonItem;
import me.mykindos.betterpvp.clans.item.cannon.CannonballItem;
import me.mykindos.betterpvp.core.item.ItemBootstrap;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

@Singleton
public class ClansItemBoostrap implements ItemBootstrap {

    private boolean registered = false;

    @Inject private Clans clans;
    @Inject private ItemRegistry itemRegistry;
    @Inject private CannonItem cannonItem;
    @Inject private CannonballItem cannonballItem;

    @Inject
    @Override
    public void registerItems() {
        if (registered) return;
        registered = true;

        final VanillaItem waterBlock = new VanillaItem("Water Block", ItemStack.of(Material.LAPIS_BLOCK), ItemRarity.UNCOMMON);
        itemRegistry.registerFallbackItem(new NamespacedKey(clans, "water_block"), Material.LAPIS_BLOCK, waterBlock);

        itemRegistry.registerItem(new NamespacedKey(clans, "cannon"), cannonItem);
        itemRegistry.registerItem(new NamespacedKey(clans, "cannonball"), cannonballItem);
    }

}
