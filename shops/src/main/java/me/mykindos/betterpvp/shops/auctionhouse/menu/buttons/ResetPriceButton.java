package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
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

public class ResetPriceButton extends ControlItem<ListingCreationMenu> {

    private final Auction auction;

    public ResetPriceButton(Auction auction) {
        this.auction = auction;
    }

    @Override
    public ItemProvider getItemProvider(ListingCreationMenu gui) {
        return ItemView.builder().material(Material.TNT_MINECART)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .displayName(Component.text("Reset", NamedTextColor.RED))
                .lore(Component.text("Left-click to reset the sell price", NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType != ClickType.LEFT) {
            return;
        }

        auction.setSellPrice(0);
        UtilMessage.simpleMessage(player, "Auction House", "Sell price reset to <yellow>$0</yellow>");
        SoundEffect.HIGH_PITCH_PLING.play(player);

        notifyWindows();
        getGui().updateControlItems();
    }
}
