package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.menu.ListingCreationMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

public class CurrentPriceButton extends ControlItem<ListingCreationMenu> {

    private final Auction auction;

    public CurrentPriceButton(Auction auction) {
        this.auction = auction;
    }

    @Override
    public ItemProvider getItemProvider(ListingCreationMenu gui) {
        return ItemView.builder().material(Material.GOLD_INGOT)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .displayName(Component.text("Sell Price", NamedTextColor.YELLOW))
                .lore(Component.text("$" + UtilFormat.formatNumber(auction.getSellPrice()), NamedTextColor.YELLOW))
                .lore(Component.text(""))
                .lore(Component.text("The auction house will take a 5% cut", NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {

    }
}
