package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionCreateEvent;
import me.mykindos.betterpvp.shops.auctionhouse.menu.ListingCreationMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

@CustomLog
public class SubmitListingButton extends ControlItem<ListingCreationMenu> {

    private final AuctionManager auctionManager;
    private final Auction auction;

    public SubmitListingButton(AuctionManager auctionManager, Auction auction) {
        this.auction = auction;
        this.auctionManager = auctionManager;
    }

    @Override
    public ItemProvider getItemProvider(ListingCreationMenu gui) {
        return ItemView.builder().material(Material.CHEST_MINECART)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .displayName(Component.text("Submit", NamedTextColor.GREEN))
                .lore(Component.text("Left-click to submit the listing.", NamedTextColor.GRAY))
                .lore(Component.text(""))
                .lore(UtilMessage.deserialize("Sell price: <green>$%s", UtilFormat.formatNumber(auction.getSellPrice())))
                .lore(UtilMessage.deserialize("Duration: <green>%s", auction.getListingDuration().getDisplay()))
                .lore(Component.text(""))
                .lore(Component.text("The auction house will take a 5% cut", NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType != ClickType.LEFT) {
            return;
        }

        AuctionCreateEvent auctionCreateEvent = UtilServer.callEvent(new AuctionCreateEvent(player, auction));
        if(auctionCreateEvent.isCancelled()) {
            UtilMessage.simpleMessage(player, "Auction House", auctionCreateEvent.getCancelReason());
            SoundEffect.WRONG_ACTION.play(player);
            return;
        }

        player.closeInventory();
        player.getInventory().remove(auction.getItemStack());

        auction.setExpiryTime(System.currentTimeMillis() + auction.getListingDuration().getDuration());
        auctionManager.getAuctionRepository().save(auction);
        auctionManager.getActiveAuctions().add(auction);

        UtilMessage.simpleMessage(player, "Auction House", "Listing created successfully.");
        log.info("{} has created a listing for {} for ${}", player.getName(), auction.getItemStack().getType().name(), auction.getSellPrice());
        SoundEffect.HIGH_PITCH_PLING.play(player);

    }
}
