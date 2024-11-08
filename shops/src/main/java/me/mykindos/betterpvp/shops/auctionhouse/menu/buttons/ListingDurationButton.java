package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.data.ListingDuration;
import me.mykindos.betterpvp.shops.auctionhouse.menu.ListingCreationMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

public class ListingDurationButton extends ControlItem<ListingCreationMenu> {

    private final Auction auction;

    public ListingDurationButton(Auction auction) {
        this.auction = auction;
    }

    @Override
    public ItemProvider getItemProvider(ListingCreationMenu gui) {
        return ItemView.builder().material(Material.CLOCK)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .displayName(Component.text("Listing Duration", NamedTextColor.YELLOW))
                .lore(Component.text(auction.getListingDuration().getDisplay(), NamedTextColor.GREEN))
                .lore(Component.text(""))
                .lore(Component.text("Left-click to cycle the duration", NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType != ClickType.LEFT) {
            return;
        }

        int index = auction.getListingDuration().ordinal();

        if (index == ListingDuration.values().length - 1) {
            index = 0;
        } else {
            index++;
        }

        auction.setListingDuration(ListingDuration.values()[index]);
        SoundEffect.HIGH_PITCH_PLING.play(player);
        notifyWindows();
        getGui().updateControlItems();

    }
}
