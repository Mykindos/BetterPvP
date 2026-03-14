package me.mykindos.betterpvp.shops.auctionhouse.listeners;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.core.item.ItemStat;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionBuyEvent;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionCreateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@BPvPListener
@Singleton
public class AuctionStatListener implements Listener {
    private final ClientManager clientManager;

    public AuctionStatListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onAuctionBuy(final AuctionBuyEvent event) {
        final Auction auction = event.getAuction();
        final ItemStack itemStack = auction.getItemStack();
        final double totalAmount = auction.getSellPrice();

        final ItemStat.ItemStatBuilder builder = ItemStat.builder()
                .itemStack(itemStack);

        final ItemStat buyCountStat = builder
                .action(ItemStat.Action.AUCTION_BUY_COUNT)
                .build();
        final ItemStat buyAmountStat = builder
                .action(ItemStat.Action.AUCTION_BUY_AMOUNT)
                .build();
        clientManager.incrementStat(event.getPlayer(), buyCountStat, 1L);
        clientManager.incrementStat(event.getPlayer(), buyAmountStat, totalAmount);

        final ItemStat sellCountStat = builder
                .action(ItemStat.Action.AUCTION_SELL_COUNT)
                .build();
        final ItemStat sellAmountStat = builder
                .action(ItemStat.Action.AUCTION_SELL_AMOUNT)
                .build();
        clientManager.incrementStatOffline(auction.getSeller(), sellCountStat, 1L);
        clientManager.incrementStatOffline(auction.getSeller(), sellAmountStat, totalAmount);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onAuctionCreate(final AuctionCreateEvent event) {
        final Auction auction = event.getAuction();
        final ItemStack itemStack = auction.getItemStack();
        final double totalAmount = auction.getSellPrice();

        final ItemStat.ItemStatBuilder builder = ItemStat.builder()
                .itemStack(itemStack);

        final ItemStat auctionCreateCountStat = builder
                .action(ItemStat.Action.AUCTION_CREATE_COUNT)
                .build();
        final ItemStat auctionCreateAmountStat = builder
                .action(ItemStat.Action.AUCTION_CREATE_AMOUNT)
                .build();
        clientManager.incrementStat(event.getPlayer(), auctionCreateCountStat, 1L);
        clientManager.incrementStat(event.getPlayer(), auctionCreateAmountStat, totalAmount);
    }
}
