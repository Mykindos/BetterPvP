package me.mykindos.betterpvp.shops.auctionhouse.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionBuyEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Iterator;

@BPvPListener
@Singleton
public class AuctionListener implements Listener {

    private final AuctionManager auctionManager;

    @Inject
    public AuctionListener(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @UpdateEvent(delay = 10000)
    public void processExpiredAuctions() {
        Iterator<Auction> auctionIterator = auctionManager.getActiveAuctions().iterator();
        while (auctionIterator.hasNext()) {
            Auction auction = auctionIterator.next();
            if (auction.hasExpired()) {
                auctionIterator.remove();
            }
        }
    }

    @EventHandler
    public void onAuctionBuy(AuctionBuyEvent event) {
        if(!auctionManager.getActiveAuctions().contains(event.getAuction())) {
            event.cancel("Auction no longer exists");
            System.out.println("AA");
        }
    }
}
