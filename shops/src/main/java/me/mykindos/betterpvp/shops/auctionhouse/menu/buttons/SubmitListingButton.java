package me.mykindos.betterpvp.shops.auctionhouse.menu.buttons;

import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
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

            Component globalSellMessage = Component.text("Auction House> ", NamedTextColor.BLUE)
                    .append(UtilMessage.deserialize("<gray>New Auction Listing: "))
                    .append(name.hoverEvent(itemStack.asHoverEvent()).append(Component.text(" for ", NamedTextColor.GRAY)
                            .append(Component.text("$" + UtilFormat.formatNumber(auction.getSellPrice()), NamedTextColor.GREEN))))
                    .clickEvent(ClickEvent.runCommand("/auctionhouse"));
            UtilMessage.broadcast(globalSellMessage);
        }

        SoundEffect.HIGH_PITCH_PLING.play(player);

    }
}
