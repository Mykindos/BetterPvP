package me.mykindos.betterpvp.clans.auctionhouse;

import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.IAuctionDeliveryService;

import java.util.UUID;

public class ClansAuctionDeliveryService implements IAuctionDeliveryService {

    private final ClanManager clanManager;

    public ClansAuctionDeliveryService(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @Override
    public void deliverAuction(UUID target, Auction auction) {
        clanManager.getClanByPlayer(target).ifPresent(clan -> {
            clan.getCore().getMailbox().getContents().add(auction.getItemStack());
            clanManager.getRepository().updateClanMailbox(clan);
        });
    }
}
