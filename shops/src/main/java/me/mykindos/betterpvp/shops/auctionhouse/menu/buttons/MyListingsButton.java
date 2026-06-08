package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.menu.AuctionHouseMenu;
import me.mykindos.betterpvp.shops.auctionhouse.menu.AuctionListingMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

public class MyListingsButton extends ControlItem<AuctionHouseMenu> {

    private final AuctionManager auctionManager;

    public MyListingsButton(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public ItemProvider getItemProvider(AuctionHouseMenu gui) {
        return ItemView.builder().material(Material.PLAYER_HEAD)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .hideAdditionalTooltip(true)
                .displayName(Translations.component("shops.menu.auction-house.button.my-listings.name").color(NamedTextColor.GREEN))
                .lore(List.of(Translations.componentLines("shops.menu.auction-house.button.my-listings.lore")))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        new AuctionListingMenu(auctionManager, new AuctionHouseMenu(auctionManager), player,
                auction -> auction.getSeller().equals(player.getUniqueId())).show(player);
    }
}
