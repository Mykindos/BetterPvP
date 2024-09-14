package me.mykindos.betterpvp.shops.auctionhouse.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.menu.buttons.CreateListingButton;
import me.mykindos.betterpvp.shops.auctionhouse.menu.buttons.ViewListingsButton;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@Singleton
public class AuctionHouseMenu extends AbstractGui implements Windowed {

    @Inject
    public AuctionHouseMenu(AuctionManager auctionManager) {
        super(9, 1);

        setItem(2, new CreateListingButton(auctionManager));
        setItem(6, new ViewListingsButton(auctionManager));
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.text("Auction House");
    }
}
