package me.mykindos.betterpvp.shops.auctionhouse;

import java.util.UUID;

public class DefaultAuctionDeliveryService implements IAuctionDeliveryService {

    @Override
    public boolean deliverAuction(UUID target, Auction auction) {
        return false;
    }

}
