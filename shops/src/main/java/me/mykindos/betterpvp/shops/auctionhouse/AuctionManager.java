package me.mykindos.betterpvp.shops.auctionhouse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionBuyEvent;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionCancelEvent;
import me.mykindos.betterpvp.shops.auctionhouse.repository.AuctionRepository;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Singleton
@Getter
@CustomLog
public class AuctionManager {

    private final ItemHandler itemHandler;
    private final ClientManager clientManager;
    private final AuctionRepository auctionRepository;

    private final List<Auction> activeAuctions = new ArrayList<>();

    @Setter
    private IAuctionDeliveryService deliveryService = new DefaultAuctionDeliveryService();

    @Inject
    public AuctionManager(ItemHandler itemHandler, ClientManager clientManager, AuctionRepository auctionRepository) {
        this.itemHandler = itemHandler;
        this.clientManager = clientManager;
        this.auctionRepository = auctionRepository;
        activeAuctions.addAll(auctionRepository.getAll());
    }

    /**
     * Lists an item on the auction house
     * @param player the player listing the auction
     * @param auction the auction being listed
     */
    public void addNewAuction(@NotNull Player player, @NotNull Auction auction) {
        auction.setExpiryTime(System.currentTimeMillis() + auction.getListingDuration().getDuration());
        getAuctionRepository().save(auction);
        getActiveAuctions().add(auction);
        itemHandler.getUUIDItem(auction.getItemStack()).ifPresent((uuidItem) -> {
            log.info("{} listed ({}) on the auction house for ${}",
                            player.getName(), uuidItem.getUuid(), auction.getSellPrice())
                    .setAction("ITEM_AUCTION_CREATE").addItemContext(uuidItem)
                    .addContext(LogContext.CURRENCY, String.valueOf(auction.getSellPrice()))
                    .addClientContext(player).submit();
        });

        UtilMessage.simpleMessage(player, "Auction House", "Listing created successfully.");
        log.info("{} has created a listing for {} for ${}", player.getName(), auction.getItemStack().getType().name(), auction.getSellPrice()).setAction("ITEM_AUCTION_LIST").submit();
    }

    public void buyAuction(Player player, Auction auction) {
        AuctionBuyEvent auctionBuyEvent = UtilServer.callEvent(new AuctionBuyEvent(player, auction));
        if (auctionBuyEvent.isCancelled()) {
            UtilMessage.simpleMessage(player, "Auction House", auctionBuyEvent.getCancelReason());
            return;
        }

        if(auctionBuyEvent.getAuction().isCancelled()) {
            UtilMessage.simpleMessage(player, "Auction House", "This auction has been cancelled.");
            return;
        }

        Client buyer = clientManager.search().online(player);
        buyer.getGamer().saveProperty(GamerProperty.BALANCE.name(), buyer.getGamer().getBalance() - auction.getSellPrice());

        auction.setSold(true);
        getAuctionRepository().setSold(auction, true);
        getAuctionRepository().saveAuctionTransaction(player.getUniqueId(), auction);

        getDeliveryService().deliverCurrency(auction.getSeller(), (int) (auction.getSellPrice() * 0.95));

        if (getDeliveryService().deliverAuction(player.getUniqueId(), auction)) {
            auction.setDelivered(true);
            getAuctionRepository().setDelivered(auction, true);
            getActiveAuctions().remove(auction);
        }

        itemHandler.getUUIDItem(auction.getItemStack()).ifPresent((uuidItem) -> {
            log.info("{} bought ({}) on the auction house for ${}",
                            player.getName(), uuidItem.getUuid(), auction.getSellPrice())
                    .setAction("ITEM_AUCTION_BUY").addItemContext(uuidItem)
                    .addContext(LogContext.CURRENCY, String.valueOf(auction.getSellPrice()))
                    .addClientContext(player).submit();
        });
    }

    public void cancelAuction(Player player, Auction auction) {
        AuctionCancelEvent auctionCancelEvent = UtilServer.callEvent(new AuctionCancelEvent(player, auction));
        if (auctionCancelEvent.isCancelled()) {
            UtilMessage.simpleMessage(player, "Auction House", auctionCancelEvent.getCancelReason());
            return;
        }

        if (auction.isSold() || auction.isDelivered()) {
            UtilMessage.simpleMessage(player, "Auction House", "This auction has already been sold.");
            return;
        }

        auction.setCancelled(true);
        getAuctionRepository().setCancelled(auction, true);

        if (getDeliveryService().deliverAuction(auction.getSeller(), auction)) {
            getAuctionRepository().setDelivered(auction, true);
            getActiveAuctions().remove(auction);
        }
        itemHandler.getUUIDItem(auction.getItemStack()).ifPresent((uuidItem) -> {
            log.info("{} canceled ({}) on the auction house",
                            player.getName(), uuidItem.getUuid(), auction.getSellPrice())
                    .setAction("ITEM_AUCTION_CANCEL").addItemContext(uuidItem)
                    .addClientContext(player).submit();
        });
    }

    public void cancelAllAuctions(UUID seller) {
        Iterator<Auction> auctionIterator = activeAuctions.iterator();
        while (auctionIterator.hasNext()) {
            Auction auction = auctionIterator.next();
            if (!auction.getSeller().equals(seller)) {
                continue;
            }

            getAuctionRepository().setCancelled(auction, true);
            if (getDeliveryService().deliverAuction(auction.getSeller(), auction)) {
                getAuctionRepository().setDelivered(auction, true);
                auctionIterator.remove();
            }
            itemHandler.getUUIDItem(auction.getItemStack()).ifPresent((uuidItem) -> {
                log.info("({})'s Auction was cancelled",
                                uuidItem.getUuid())
                        .setAction("ITEM_AUCTION_CANCEL").addItemContext(uuidItem).submit();
            });
        }
    }

    public boolean deliverAuction(UUID target, Auction auction) {
        if (deliveryService.deliverAuction(target, auction)) {
            auction.setDelivered(true);
            auctionRepository.setDelivered(auction, true);

            return true;
        }

        return false;
    }

}
