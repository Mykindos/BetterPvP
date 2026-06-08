package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.events.AuctionCreateEvent;
import me.mykindos.betterpvp.shops.auctionhouse.menu.ListingCreationMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

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
        Component priceArg = Component.text(UtilFormat.formatNumber(auction.getSellPrice()), NamedTextColor.GREEN);
        Component durationArg = Component.text(auction.getListingDuration().getDisplay(), NamedTextColor.GREEN);
        return ItemView.builder().material(Material.CHEST_MINECART)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .displayName(Translations.component("shops.menu.listing-creation.button.submit.name").color(NamedTextColor.GREEN))
                .lore(Translations.component("shops.menu.listing-creation.button.submit.lore.1").color(NamedTextColor.GRAY))
                .lore(Component.text(""))
                .lore(Translations.component("shops.menu.listing-creation.button.submit.lore.2", priceArg).color(NamedTextColor.GRAY))
                .lore(Translations.component("shops.menu.listing-creation.button.submit.lore.3", durationArg).color(NamedTextColor.GRAY))
                .lore(Component.text(""))
                .lore(Translations.component("shops.menu.listing-creation.button.submit.lore.4").color(NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickType != ClickType.LEFT) {
            return;
        }

        AuctionCreateEvent auctionCreateEvent = UtilServer.callEvent(new AuctionCreateEvent(player, auction));
        if(auctionCreateEvent.isCancelled()) {
            UtilMessage.message(player, "core.prefix.auction-house", auctionCreateEvent.getCancelReason());
            SoundEffect.WRONG_ACTION.play(player);
            return;
        }

        player.closeInventory();

        ItemStack itemStack = auction.getItemStack();
        if (UtilInventory.remove(player, itemStack)) {
            auctionManager.addNewAuction(player, auction);

            ItemFactory itemFactory = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(ItemFactory.class);
            final Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(itemStack);
            final Component name;
            if (instanceOpt.isPresent()) {
                final ItemInstance instance = instanceOpt.get();
                name = instance.getBaseItem().getItemNameRenderer().createName(instance);
                itemStack = instance.getView().get();
            } else {
                if (itemStack.getItemMeta().hasDisplayName()) {
                    name = Objects.requireNonNull(itemStack.getItemMeta().displayName());
                } else {
                    name = Objects.requireNonNullElse(itemStack.getData(DataComponentTypes.ITEM_NAME),
                            Component.translatable(itemStack.getType().translationKey()));
                }
            }

            final ItemStack hoverStack = itemStack;
            final double sellPrice = auction.getSellPrice();
            // Per recipient: render the hover ItemStack into each viewer's locale before building the hover.
            UtilMessage.broadcastLocalized(locale -> {
                Component newListingLabel = Translations.render(Translations.component("shops.menu.listing-creation.broadcast.new-listing").color(NamedTextColor.GRAY), locale);
                Component forLabel = Translations.render(Translations.component("shops.menu.listing-creation.broadcast.for").color(NamedTextColor.GRAY), locale);
                return Component.text("Auction House> ", NamedTextColor.BLUE)
                        .append(newListingLabel)
                        .appendSpace()
                        .append(name.hoverEvent(Translations.renderItemStack(hoverStack, locale)))
                        .appendSpace()
                        .append(forLabel)
                        .appendSpace()
                        .append(Component.text("$" + UtilFormat.formatNumber(sellPrice), NamedTextColor.GREEN))
                        .clickEvent(ClickEvent.runCommand("/auctionhouse"));
            });
        }

        SoundEffect.HIGH_PITCH_PLING.play(player);

    }
}
