package me.mykindos.betterpvp.shops.auctionhouse.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.menu.AuctionHouseMenu;
import org.bukkit.entity.Player;

@Singleton
public class CancelAllAuctionsCommand extends Command {

    private final AuctionManager auctionManager;

    @Inject
    public CancelAllAuctionsCommand(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String getName() {
        return "cancelallauctions";
    }

    @Override
    public String getDescription() {
        return "Cancel all auctions";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        auctionManager.getActiveAuctions().forEach(auction -> {
            auction.setCancelled(true);
            auctionManager.getAuctionRepository().setCancelled(auction, true);

            if (auctionManager.getDeliveryService().deliverAuction(auction.getSeller(), auction)) {
                auctionManager.getAuctionRepository().setDelivered(auction, true);

            }
        });

        auctionManager.getActiveAuctions().clear();
    }
}
