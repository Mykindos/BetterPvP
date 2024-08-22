package me.mykindos.betterpvp.shops.auctionhouse.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class AuctionBuyEvent extends CustomCancellableEvent {

    private final Player player;
    private final Auction auction;

}
