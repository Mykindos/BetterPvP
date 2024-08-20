package me.mykindos.betterpvp.shops.auctionhouse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionBuyEvent;
import me.mykindos.betterpvp.shops.auctionhouse.repository.AuctionRepository;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Singleton
@Getter
public class AuctionManager {

    private final AuctionRepository auctionRepository;

    private final List<Auction> activeAuctions = new ArrayList<>();

    @Setter
    private IAuctionDeliveryService deliveryService = new DefaultAuctionDeliveryService();

    @Inject
    public AuctionManager(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
        activeAuctions.addAll(auctionRepository.getAll());
    }

    public void buyAuction(Player player, Auction auction) {
        AuctionBuyEvent auctionBuyEvent = UtilServer.callEvent(new AuctionBuyEvent(player, auction));
        if (auctionBuyEvent.isCancelled()) {
            // TODO tell player why
            return;
        }
        System.out.println("A");

        getAuctionRepository().setSold(auction, true);
        // TODO insert into auction transactions
        getActiveAuctions().remove(auction);
        getDeliveryService().deliverAuction(player.getUniqueId(), auction);
        getAuctionRepository().setDelivered(auction, true);
    }

}
