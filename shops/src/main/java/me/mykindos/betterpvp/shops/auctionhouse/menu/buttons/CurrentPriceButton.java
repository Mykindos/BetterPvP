package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.menu.ListingCreationMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.List;
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
                .displayName(Translations.component("shops.menu.listing-creation.button.current-price.name").color(NamedTextColor.YELLOW))
                .lore(Component.text("$" + UtilFormat.formatNumber(auction.getSellPrice()), NamedTextColor.YELLOW))
                .lore(Component.text(""))
                .lore(List.of(Translations.componentLines("shops.menu.listing-creation.button.current-price.lore")))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {

    }
}
