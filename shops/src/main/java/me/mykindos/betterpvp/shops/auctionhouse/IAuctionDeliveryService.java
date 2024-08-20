package me.mykindos.betterpvp.shops.auctionhouse;

import java.util.UUID;

public interface IAuctionDeliveryService {

    void deliverAuction(UUID target, Auction auction);

}
