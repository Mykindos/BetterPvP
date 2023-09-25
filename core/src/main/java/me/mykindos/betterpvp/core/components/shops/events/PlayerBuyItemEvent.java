package me.mykindos.betterpvp.core.components.shops.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.components.shops.ShopCurrency;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlayerBuyItemEvent extends CustomCancellableEvent {

    private final Player player;
    private final Gamer gamer;
    private final IShopItem shopItem;
    private final ItemStack item;
    private final ShopCurrency currency;
    private final ClickType clickType;

}
