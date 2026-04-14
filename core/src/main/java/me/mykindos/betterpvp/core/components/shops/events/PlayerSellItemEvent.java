package me.mykindos.betterpvp.core.components.shops.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.components.shops.ShopCurrency;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.inventory.inventory.Inventory;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlayerSellItemEvent extends CustomCancellableEvent {

    private final Player player;
    private final Gamer gamer;
    private final IShopItem shopItem;
    /**
     * The inventory from which matching items will be taken.
     * For a normal shop sell this is a {@link me.mykindos.betterpvp.core.inventory.inventory.ReferencingInventory}
     * wrapping the player's storage contents; for a staged sell it is the {@link me.mykindos.betterpvp.core.inventory.inventory.VirtualInventory}
     * shown in the sell-all menu.
     */
    private final Inventory inventory;
    private final ShopCurrency currency;
    private int requestedAmount = -1;

}
