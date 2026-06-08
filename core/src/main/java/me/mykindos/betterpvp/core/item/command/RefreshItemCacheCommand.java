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
        return "core.command.refresh-item-cache.description";
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        GuiItemViewer.invalidateCache();
        UtilMessage.message(player, "core.prefix.command", "core.command.refreshitemcache.success");
    }
}
