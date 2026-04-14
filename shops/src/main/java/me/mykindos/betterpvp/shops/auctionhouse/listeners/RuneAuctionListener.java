package me.mykindos.betterpvp.shops.auctionhouse.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.shops.auctionhouse.events.PlayerPrepareListingEvent;
import me.mykindos.betterpvp.shops.shops.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Prevents players from listing items in the auction house that do not have runes applied,
 * if they have a rune container component.
 */
@Singleton
@BPvPListener
public class RuneAuctionListener implements Listener {

    private final ItemFactory itemFactory;
    private final ShopManager shopManager;

    @Inject
    private RuneAuctionListener(ItemFactory itemFactory, ShopManager shopManager) {
        this.itemFactory = itemFactory;
        this.shopManager = shopManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onUncommonItems(PlayerPrepareListingEvent event) {
        ItemStack itemStack = event.getItemStack();
        if (!UtilItem.isTool(itemStack) && !UtilItem.isWeapon(itemStack) && !UtilItem.isArmour(itemStack.getType())){
            return;
        }

        itemFactory.fromItemStack(itemStack).ifPresent(item -> {
            if (item.getRarity().isAtLeast(ItemRarity.RARE)) {
                Bukkit.broadcastMessage("test1");
                return;
            }

            Bukkit.broadcastMessage("test");
            item.getComponent(RuneContainerComponent.class).ifPresent(container -> {
                if (container.getRunes().isEmpty()) {
                    event.cancel("You cannot list this type of item unless it has a rune applied.");
                }
            });
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onShopItems(PlayerPrepareListingEvent event) {
        ItemStack itemStack = event.getItemStack();
        final ItemInstance itemInstance = itemFactory.fromItemStack(itemStack).orElseThrow();
        final NamespacedKey key = Objects.requireNonNull(itemFactory.getItemRegistry().getKey(itemInstance.getBaseItem()));
        final List<IShopItem> matchingShopItems = shopManager.getShopItems().values()
                .stream()
                .flatMap(Collection::stream)
                .filter(shopItem -> shopItem.getItemKey().equals(key.toString()))
                .toList();
        if (matchingShopItems.isEmpty()) {
            return; // not a shop item
        }

        // if it is a shop item and it doesnt have runes, prevent it from being listed
        final Optional<RuneContainerComponent> containerOpt = itemInstance.getComponent(RuneContainerComponent.class);
        if (containerOpt.isPresent()) {
            if (containerOpt.get().getRunes().isEmpty()) {
                event.cancel("You cannot list this type of item unless it has a rune applied.");
            }
        } else {
            event.cancel("You cannot list items sold at shops.");
        }
    }

}
