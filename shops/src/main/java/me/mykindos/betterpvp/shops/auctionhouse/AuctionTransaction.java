package me.mykindos.betterpvp.shops.auctionhouse;

import lombok.Data;

import java.util.UUID;

@Data
public class AuctionTransaction {

    private final UUID auctionId;
    private final UUID buyer;

}
