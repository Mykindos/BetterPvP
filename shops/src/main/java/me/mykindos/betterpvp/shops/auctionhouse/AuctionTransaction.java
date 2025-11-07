package me.mykindos.betterpvp.shops.auctionhouse;

import lombok.Data;

import java.util.UUID;

@Data
public class AuctionTransaction {

    private final long auctionId;
    private final UUID buyer;

}
