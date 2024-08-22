package me.mykindos.betterpvp.shops.auctionhouse.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionBuyEvent;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionCreateEvent;
import me.mykindos.betterpvp.shops.auctionhouse.menu.AuctionHouseMenu;
import me.mykindos.betterpvp.shops.shops.shopkeepers.ShopkeeperManager;
import me.mykindos.betterpvp.shops.shops.shopkeepers.types.IShopkeeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Iterator;
import java.util.Optional;

@BPvPListener
@Singleton
public class AuctionListener implements Listener {

    private final ClientManager clientManager;
    private final AuctionManager auctionManager;
    private final ShopkeeperManager shopkeeperManager;

    @Inject
    public AuctionListener(ClientManager clientManager, AuctionManager auctionManager, ShopkeeperManager shopkeeperManager) {
        this.clientManager = clientManager;
        this.auctionManager = auctionManager;
        this.shopkeeperManager = shopkeeperManager;
    }

    @UpdateEvent(delay = 10000)
    public void processAuctions() {
        Iterator<Auction> auctionIterator = auctionManager.getActiveAuctions().iterator();
        while (auctionIterator.hasNext()) {
            Auction auction = auctionIterator.next();
            if (auction.hasExpired()) {
                auctionManager.deliverAuction(auction.getSeller(), auction);
                auctionIterator.remove();
            } else if ((auction.isSold() || auction.isCancelled()) && !auction.isDelivered()) {
                if (auction.getTransaction() != null) {
                    auctionManager.deliverAuction(auction.getTransaction().getBuyer(), auction);
                    auctionIterator.remove();
                }
            }
        }
    }

    @EventHandler
    public void onAuctionBuy(AuctionBuyEvent event) {
        Client client = clientManager.search().online(event.getPlayer());
        if(client.getGamer().getBalance() < event.getAuction().getSellPrice()) {
            event.cancel("You do not have enough money to purchase this item.");
            return;
        }

        if (!auctionManager.getActiveAuctions().contains(event.getAuction())) {
            event.cancel("This auction no longer exists.");
        }
    }

    @EventHandler
    public void onAuctionCreate(AuctionCreateEvent event) {
        if(event.getAuction().getSellPrice() <= 0) {
            event.cancel("You must set a sell price greater than $0.");
            return;
        }

        if(event.getAuction().getSellPrice() > 1_000_000_000) {
            event.cancel("You cannot set a sell price greater than $1,000,000,000.");
            return;
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (!(event.getRightClicked() instanceof LivingEntity target)) return;


        Optional<IShopkeeper> shopkeeperOptional = shopkeeperManager.getObject(target.getUniqueId());
        shopkeeperOptional.ifPresent(shopkeeper -> {
            if (shopkeeper.getShopkeeperName().toLowerCase().contains("auction")) {
                new AuctionHouseMenu(auctionManager).show(event.getPlayer());
            }
        });
    }
}
