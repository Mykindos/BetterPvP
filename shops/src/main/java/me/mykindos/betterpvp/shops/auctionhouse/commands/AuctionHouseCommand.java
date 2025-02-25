package me.mykindos.betterpvp.shops.auctionhouse.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.shops.auctionhouse.AuctionManager;
import me.mykindos.betterpvp.shops.auctionhouse.menu.AuctionHouseMenu;
import org.bukkit.entity.Player;

@Singleton
public class AuctionHouseCommand extends Command {

    private final AuctionManager auctionManager;

    @Inject
    public AuctionHouseCommand(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
        getAliases().add("ah");
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
        if(!client.isAdministrating()) {
            if (client.getGamer().isInCombat()) {
               UtilMessage.simpleMessage(player,"Auction House", "You cannot access the auction house while in combat.");
                return;
            }

            if(!player.getWorld().getName().equalsIgnoreCase("world")) {
                UtilMessage.simpleMessage(player,"Auction House", "You cannot access the auction house in this world.");
                return;
            }

        }
        new AuctionHouseMenu(auctionManager).show(player);
    }
}
