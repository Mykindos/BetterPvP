package me.mykindos.betterpvp.core.components.shops.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.components.shops.IShopItem;
import me.mykindos.betterpvp.core.components.shops.ShopCurrency;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@EqualsAndHashCode(callSuper = true)
@Data
public class FinalPlayerSellItemEvent extends CustomEvent {
    private final Player player;
    private final Gamer gamer;
    private final IShopItem shopItem;
    private final ItemStack item;
    private final ShopCurrency currency;
    private final int count;
    private final double totalAmount;
}
