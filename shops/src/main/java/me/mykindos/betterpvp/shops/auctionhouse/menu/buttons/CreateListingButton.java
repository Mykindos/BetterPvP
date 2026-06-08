package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.locale.Translations;
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

import java.util.List;
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
                .displayName(Translations.component("shops.menu.auction-house.button.create-listing.name").color(NamedTextColor.GREEN))
                .lore(List.of(Translations.componentLines("shops.menu.auction-house.button.create-listing.lore")))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand().clone();
        if (itemInMainHand.getType().isAir()) {
            UtilMessage.message(player, "core.prefix.auction-house", Translations.component("shops.menu.listing-creation.message.no-item"));
            return;
        }

        if (itemInMainHand.getType().isBlock()) {
            UtilMessage.message(player, "core.prefix.auction-house", Translations.component("shops.menu.listing-creation.message.no-blocks"));
            return;
        }

        if (itemInMainHand.getType() == Material.MAP || itemInMainHand.getType() == Material.FILLED_MAP) {
            UtilMessage.message(player, "core.prefix.auction-house", Translations.component("shops.menu.listing-creation.message.no-maps"));
            return;
        }

        PlayerPrepareListingEvent playerPrepareListingEvent = UtilServer.callEvent(new PlayerPrepareListingEvent(player, itemInMainHand));
        if (!playerPrepareListingEvent.isCancelled()) {
            new ListingCreationMenu(auctionManager, player.getUniqueId(), itemInMainHand.clone()).show(player);
        } else {
            UtilMessage.message(player, "core.prefix.auction-house", playerPrepareListingEvent.getCancelReason());
        }
    }
}
