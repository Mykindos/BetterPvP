package me.mykindos.betterpvp.shops.auctionhouse;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.shops.auctionhouse.data.ListingDuration;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

@Data
@RequiredArgsConstructor
public class Auction {

    private final UUID auctionID;
    private final UUID seller;
    private final ItemStack itemStack;
    private int sellPrice;
    private ListingDuration listingDuration = ListingDuration.ONE_DAY;
    private long expiryTime;
    private boolean sold;
    private boolean cancelled;
    private boolean delivered;
    private AuctionTransaction transaction;

    public Auction(UUID seller, ItemStack itemStack) {
        this(UUID.randomUUID(), seller, itemStack);
    }

    public boolean hasExpired() {
        return System.currentTimeMillis() >= expiryTime;
    }
}
