package me.mykindos.betterpvp.shops.auctionhouse.menu;

import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.BackButton;
import me.mykindos.betterpvp.shops.auctionhouse.Auction;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.menu.buttons.AddPriceButton;
import me.mykindos.betterpvp.shops.auctionhouse.menu.buttons.CurrentPriceButton;
import me.mykindos.betterpvp.shops.auctionhouse.menu.buttons.ListingDurationButton;
import me.mykindos.betterpvp.shops.auctionhouse.menu.buttons.ListingItemButton;
import me.mykindos.betterpvp.shops.auctionhouse.menu.buttons.ResetPriceButton;
import me.mykindos.betterpvp.shops.auctionhouse.menu.buttons.SubmitListingButton;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ListingCreationMenu extends AbstractGui implements Windowed {

    private final AuctionManager auctionManager;
    private final Auction auction;

    public ListingCreationMenu(AuctionManager auctionManager, UUID seller, ItemStack itemStack) {
        super(9, 5);
        this.auctionManager = auctionManager;
        this.auction = new Auction(seller, itemStack);

        setItem(10, new CurrentPriceButton(auction));
        setItem(19, new ListingItemButton(auction));
        setItem(28, new ListingDurationButton(auction));

        setItem(14, new ResetPriceButton(auction));
        setItem(21, new AddPriceButton(auction, Material.LIME_CONCRETE, 100));
        setItem(22, new AddPriceButton(auction, Material.LIME_CONCRETE, 1000));
        setItem(23, new AddPriceButton(auction, Material.LIME_TERRACOTTA, 10000));
        setItem(24, new AddPriceButton(auction, Material.GREEN_CONCRETE, 100000));
        setItem(25, new AddPriceButton(auction, Material.GREEN_CONCRETE, 1000000));
        setItem(32, new SubmitListingButton(auctionManager, auction));

        setItem(44, new BackButton(new AuctionHouseMenu(auctionManager)));


    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Create Listing");
    }
}
