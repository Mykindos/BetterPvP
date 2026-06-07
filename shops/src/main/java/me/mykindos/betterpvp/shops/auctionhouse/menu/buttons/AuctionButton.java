package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.impl.PaginatedLoreItem;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
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

/**
 * A listing tile in the auction house: a swap-key {@link PaginatedLoreItem} (the viewer flips lore
 * pages with the offhand key) that additionally purchases on left click and, for the seller/ops,
 * cancels on right click. The price/expiry lines and click hints are appended by the decorator.
 */
public class AuctionButton extends PaginatedLoreItem {

    private static final PrettyTime prettyTime = new PrettyTime();

    private final AuctionManager auctionManager;
    private final Auction auction;

    public AuctionButton(AuctionManager auctionManager, Auction auction, Player viewer) {
        super(auctionManager.getItemFactory().fromItemStack(auction.getItemStack()).orElseThrow(),
                stack -> decorate(stack, auction, viewer));
        this.auctionManager = auctionManager;
        this.auction = auction;
    }

    private static ItemStack decorate(ItemStack itemStack, Auction auction, Player viewer) {
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
            lore.add(ClickActions.LEFT.to(Component.text("Purchase")));
            if (auction.getSeller().equals(viewer.getUniqueId()) || viewer.isOp()) {
                lore.add(ClickActions.RIGHT.to(Component.text("Cancel")));
            }

            meta.lore(lore);
        });
        return itemStack;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        // Let the base item flip the lore page on the offhand/swap key (F).
        super.handleClick(clickType, player, event);

        if (clickType.isLeftClick()) {
            new ConfirmationMenu("Are you sure you want to purchase this item?", (confirm) -> {
                if (Boolean.TRUE.equals(confirm)) {
                    auctionManager.buyAuction(player, auction);
                }
            }).show(player);
        } else if (clickType.isRightClick()) {
            if (auction.getSeller().equals(player.getUniqueId()) || player.isOp()) {
                new ConfirmationMenu("Are you sure you want to cancel this auction?", (confirm) -> {
                    if (Boolean.TRUE.equals(confirm)) {
                        auctionManager.cancelAuction(player, auction);
                    }
                }).show(player);
            }
        }
    }
}
