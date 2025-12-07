package me.mykindos.betterpvp.shops.auctionhouse.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.shops.auctionhouse.events.PlayerPrepareListingEvent;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents players from listing items in the auction house that do not have runes applied,
 * if they have a rune container component.
 */
@Singleton
@BPvPListener
public class RuneAuctionListener implements Listener {

    private final ItemFactory itemFactory;

    @Inject
    private RuneAuctionListener(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareListing(PlayerPrepareListingEvent event) {
        ItemStack itemStack = event.getItemStack();
        if (!UtilItem.isTool(itemStack) && !UtilItem.isWeapon(itemStack) && !UtilItem.isArmour(itemStack.getType())){
            return;
        }

        itemFactory.fromItemStack(itemStack).ifPresent(item -> {
            if (item.getRarity().isAtLeast(ItemRarity.UNCOMMON)) {
                return;
            }

            item.getComponent(RuneContainerComponent.class).ifPresent(container -> {
                if (container.getRunes().isEmpty()) {
                    event.cancel("You cannot list this type of item unless it has a rune applied.");
                }
            });
        });
    }

}
