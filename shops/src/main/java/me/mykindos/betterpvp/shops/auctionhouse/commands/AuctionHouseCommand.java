package me.mykindos.betterpvp.shops.auctionhouse.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.menu.AuctionHouseMenu;
import org.bukkit.entity.Player;

@Singleton
public class AuctionHouseCommand extends Command {

    private final AuctionManager auctionManager;

    @Inject
    public AuctionHouseCommand(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String getName() {
        return "auctionhouse";
    }

    @Override
    public String getDescription() {
        return "Open the auction house menu";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        new AuctionHouseMenu(auctionManager).show(player);
    }
}
