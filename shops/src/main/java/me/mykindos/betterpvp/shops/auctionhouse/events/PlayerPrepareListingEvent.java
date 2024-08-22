package me.mykindos.betterpvp.shops.auctionhouse.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlayerPrepareListingEvent extends CustomCancellableEvent {

    private final Player player;
    private final ItemStack itemStack;

}
