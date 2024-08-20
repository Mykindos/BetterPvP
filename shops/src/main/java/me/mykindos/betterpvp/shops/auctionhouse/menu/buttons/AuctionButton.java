package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.menu.AuctionListingMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AuctionButton extends ControlItem<AuctionListingMenu> {

    private static final PrettyTime prettyTime = new PrettyTime();

    private final AuctionManager auctionManager;
    private final Auction auction;

    public AuctionButton(AuctionManager auctionManager, Auction auction) {
        this.auctionManager = auctionManager;
        this.auction = auction;
    }

    @Override
    public ItemProvider getItemProvider(AuctionListingMenu gui) {

        ItemStack itemStack = auction.getItemStack().clone();
        itemStack.editMeta(meta -> {
            List<Component> lore = meta.lore();
            if (lore == null) {
                lore = new ArrayList<>();
            }

            lore.add(Component.text(""));
            lore.add(UtilMessage.DIVIDER);
            lore.add(Component.text("Price: ", NamedTextColor.WHITE).append(Component.text("$" + UtilFormat.formatNumber(auction.getSellPrice()), NamedTextColor.YELLOW)));

            lore.add(Component.text("Expires: ", NamedTextColor.WHITE).append(Component.text(prettyTime.format(new Date(auction.getExpiryTime())), NamedTextColor.YELLOW)));
            lore.add(Component.text(""));
            lore.add(Component.text(ClickActions.LEFT.getName() + " to ", NamedTextColor.WHITE).append(Component.text("Purchase", NamedTextColor.YELLOW)));
            meta.lore(lore);

        });

        return ItemView.of(itemStack);
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {

        if (clickType.isLeftClick()) {
            new ConfirmationMenu("Are you sure you want to purchase this item?", (confirm) -> {
                if (Boolean.TRUE.equals(confirm)) {
                    auctionManager.buyAuction(player, auction);
                }
            }).show(player);
        }
    }
}
