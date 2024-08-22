package me.mykindos.betterpvp.shops.auctionhouse;

import java.util.UUID;

public interface IAuctionDeliveryService {

    default boolean deliverAuction(UUID target, Auction auction) {
        return false;
    }

    default boolean deliverCurrency(UUID target, int amount) {
        return false;
    }

}
