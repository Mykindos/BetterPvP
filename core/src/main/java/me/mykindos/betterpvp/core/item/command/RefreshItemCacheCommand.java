package me.mykindos.betterpvp.core.item.command;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.item.menu.viewer.GuiItemViewer;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class RefreshItemCacheCommand extends Command {

    @Override
    public String getName() {
        return "refreshitemcache";
    }

    @Override
    public String getDescription() {
        return "Rebuild the item viewer cache (use after editing a registered item's shape)";
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        GuiItemViewer.invalidateCache();
        UtilMessage.simpleMessage(player, "Items", "Item viewer cache invalidated. Next /items will rebuild it.");
    }
}
