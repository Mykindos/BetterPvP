package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.events.PlayerPrepareListingEvent;
import me.mykindos.betterpvp.shops.auctionhouse.menu.AuctionHouseMenu;
import me.mykindos.betterpvp.shops.auctionhouse.menu.ListingCreationMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CreateListingButton extends ControlItem<AuctionHouseMenu> {

    private final AuctionManager auctionManager;

    public CreateListingButton(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public ItemProvider getItemProvider(AuctionHouseMenu gui) {
        return ItemView.builder().material(Material.MACE)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .displayName(Component.text("Create Listing", NamedTextColor.GREEN))
                .lore(Component.text("Click to create a listing with your current item", NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        if(itemInMainHand.getType().isAir()) {
            UtilMessage.simpleMessage(player, "Auction House", "You must be holding an item to create a listing.");
            return;
        }

        PlayerPrepareListingEvent playerPrepareListingEvent = UtilServer.callEvent(new PlayerPrepareListingEvent(player, itemInMainHand));
        if (!playerPrepareListingEvent.isCancelled()) {
            new ListingCreationMenu(auctionManager, player.getUniqueId(), itemInMainHand).show(player);
        } else {
            UtilMessage.simpleMessage(player, "Auction House", playerPrepareListingEvent.getCancelReason());
        }
    }
}
