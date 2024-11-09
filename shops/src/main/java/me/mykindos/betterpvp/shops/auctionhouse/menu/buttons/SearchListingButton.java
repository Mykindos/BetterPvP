package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.Shops;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.menu.AuctionHouseMenu;
import me.mykindos.betterpvp.shops.auctionhouse.menu.AuctionListingMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class SearchListingButton extends ControlItem<AuctionListingMenu> {

    private final AuctionManager auctionManager;

    public SearchListingButton(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public ItemProvider getItemProvider(AuctionListingMenu gui) {
        return ItemView.builder().material(Material.SPYGLASS)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .displayName(Component.text("Search", NamedTextColor.GREEN))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        player.setMetadata("auction-search", new FixedMetadataValue(JavaPlugin.getPlugin(Shops.class), "Auction Search"));
        UtilMessage.simpleMessage(player, "Auction House", "What would you like to search for?");
        player.closeInventory();
    }
}
