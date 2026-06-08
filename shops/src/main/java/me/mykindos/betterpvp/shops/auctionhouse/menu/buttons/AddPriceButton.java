package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.menu.ListingCreationMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.List;
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
        Component priceArg = Component.text(formattedPrice, NamedTextColor.GREEN);
        return ItemView.builder().material(material)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .displayName(Translations.component("shops.menu.listing-creation.button.add-price.name", priceArg).color(NamedTextColor.GREEN))
                .lore(List.of(Translations.componentLines("shops.menu.listing-creation.button.add-price.lore", priceArg)))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType != ClickType.LEFT) {
            return;
        }

        auction.setSellPrice(auction.getSellPrice() + price);
        UtilMessage.message(player, "core.prefix.auction-house", Translations.component("shops.menu.listing-creation.message.price-increased",
                Component.text(UtilFormat.formatNumber(price), NamedTextColor.YELLOW),
                Component.text(UtilFormat.formatNumber(auction.getSellPrice()), NamedTextColor.YELLOW)));

        SoundEffect.HIGH_PITCH_PLING.play(player);

        notifyWindows();
        getGui().updateControlItems();
    }


}
