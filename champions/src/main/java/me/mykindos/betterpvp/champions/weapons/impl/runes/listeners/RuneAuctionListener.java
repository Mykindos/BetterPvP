package me.mykindos.betterpvp.champions.weapons.impl.runes.listeners;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.weapons.impl.runes.RuneNamespacedKeys;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.shops.auctionhouse.events.PlayerPrepareListingEvent;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

@Singleton
@PluginAdapter("Shops")
@BPvPListener
public class RuneAuctionListener implements Listener {

    @EventHandler (ignoreCancelled = true)
    public void onPrepareListing(PlayerPrepareListingEvent event) {

        ItemStack itemStack = event.getItemStack();
        if(itemStack.getType() == Material.NETHERITE_AXE || itemStack.getType() == Material.NETHERITE_SWORD){
            return;
        }

        if(!UtilItem.isTool(itemStack) && !UtilItem.isWeapon(itemStack) && !UtilItem.isArmour(itemStack.getType())){
            return;
        }

        PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
        if(!pdc.has(RuneNamespacedKeys.HAS_RUNE)) {
            event.cancel("You cannot list this type of item unless it has a rune applied.");
        }
    }

}
