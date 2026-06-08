package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.locale.Translations;
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

public class ResetPriceButton extends ControlItem<ListingCreationMenu> {

    private final Auction auction;

    public ResetPriceButton(Auction auction) {
        this.auction = auction;
    }

    @Override
    public ItemProvider getItemProvider(ListingCreationMenu gui) {
        return ItemView.builder().material(Material.TNT_MINECART)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .displayName(Translations.component("shops.menu.listing-creation.button.reset-price.name").color(NamedTextColor.RED))
                .lore(List.of(Translations.componentLines("shops.menu.listing-creation.button.reset-price.lore")))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType != ClickType.LEFT) {
            return;
        }

        auction.setSellPrice(0);
        UtilMessage.message(player, "core.prefix.auction-house", Translations.component("shops.menu.listing-creation.message.price-reset"));
        SoundEffect.HIGH_PITCH_PLING.play(player);

        notifyWindows();
        getGui().updateControlItems();
    }
}
