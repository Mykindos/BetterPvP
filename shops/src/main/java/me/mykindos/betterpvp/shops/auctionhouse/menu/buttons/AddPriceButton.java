package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
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

public class AddPriceButton extends ControlItem<ListingCreationMenu> {

    private final Auction auction;
    private final Material material;
    private final int price;

    public AddPriceButton(Auction auction, Material material, int price) {
        this.auction = auction;
        this.material = material;
        this.price = price;
    }

    @Override
    public ItemProvider getItemProvider(ListingCreationMenu gui) {
        String formattedPrice = UtilFormat.formatNumber(price);
        return ItemView.builder().material(material)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .displayName(Component.text("+ $" + formattedPrice, NamedTextColor.GREEN))
                .lore(Component.text("Left-click increase sell price by $" + formattedPrice, NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType != ClickType.LEFT) {
            return;
        }

        auction.setSellPrice(auction.getSellPrice() + price);
        UtilMessage.simpleMessage(player, "Auction House", "Sell price increased by <yellow>%s</yellow> to <yellow>%s",
                "$" +  UtilFormat.formatNumber(price), "$" + UtilFormat.formatNumber(auction.getSellPrice()));

        SoundEffect.HIGH_PITCH_PLING.play(player);

        notifyWindows();
        getGui().updateControlItems();
    }


}
