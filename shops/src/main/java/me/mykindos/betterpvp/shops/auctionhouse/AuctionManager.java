package me.mykindos.betterpvp.shops.auctionhouse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionBuyEvent;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionCancelEvent;
import me.mykindos.betterpvp.shops.auctionhouse.repository.AuctionRepository;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Singleton
@Getter
public class AuctionManager {

    private final ClientManager clientManager;
    private final AuctionRepository auctionRepository;

    private final List<Auction> activeAuctions = new ArrayList<>();

    @Setter
    private IAuctionDeliveryService deliveryService = new DefaultAuctionDeliveryService();

    @Inject
    public AuctionManager(ClientManager clientManager, AuctionRepository auctionRepository) {
        this.clientManager = clientManager;
        this.auctionRepository = auctionRepository;
        activeAuctions.addAll(auctionRepository.getAll());
    }

    public void buyAuction(Player player, Auction auction) {
        AuctionBuyEvent auctionBuyEvent = UtilServer.callEvent(new AuctionBuyEvent(player, auction));
        if (auctionBuyEvent.isCancelled()) {
            UtilMessage.simpleMessage(player, "Auction House", auctionBuyEvent.getCancelReason());
            return;
        }

        Client buyer = clientManager.search().online(player);
        buyer.getGamer().saveProperty(GamerProperty.BALANCE.name(), buyer.getGamer().getBalance() - auction.getSellPrice());

        getAuctionRepository().setSold(auction, true);
        getAuctionRepository().saveAuctionTransaction(player.getUniqueId(), auction);

        getDeliveryService().deliverCurrency(auction.getSeller(), (int) (auction.getSellPrice() * 0.95));

        if(getDeliveryService().deliverAuction(player.getUniqueId(), auction)) {
            getAuctionRepository().setDelivered(auction, true);
            getActiveAuctions().remove(auction);
        }
    }

    public void cancelAuction(Player player, Auction auction) {
        AuctionCancelEvent auctionCancelEvent = UtilServer.callEvent(new AuctionCancelEvent(player, auction));
        if (auctionCancelEvent.isCancelled()) {
            UtilMessage.simpleMessage(player, "Auction House", auctionCancelEvent.getCancelReason());
            return;
        }

        if(auction.isSold()) {
            UtilMessage.simpleMessage(player, "Auction House", "This auction has already been sold.");
            return;
        }

        getAuctionRepository().setCancelled(auction, true);

        if(getDeliveryService().deliverAuction(auction.getSeller(), auction)) {
            getAuctionRepository().setDelivered(auction, true);
            getActiveAuctions().remove(auction);
        }
    }

    public void cancelAllAuctions(UUID seller) {
        Iterator<Auction> auctionIterator = activeAuctions.iterator();
        while(auctionIterator.hasNext()) {
            Auction auction = auctionIterator.next();
            if(!auction.getSeller().equals(seller)) {
                continue;
            }

            getAuctionRepository().setCancelled(auction, true);
            if(getDeliveryService().deliverAuction(auction.getSeller(), auction)) {
                getAuctionRepository().setDelivered(auction, true);
                auctionIterator.remove();
            }
        }
    }

    public void deliverAuction(UUID target, Auction auction) {
        if(deliveryService.deliverAuction(target, auction)) {
            auctionRepository.setDelivered(auction, true);
        }
    }

}
