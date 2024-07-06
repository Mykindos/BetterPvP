package me.mykindos.betterpvp.core.wiki.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.wiki.menus.WikiMenu;
import org.bukkit.entity.Player;

@Singleton
public class WikiCommand extends Command {

    private final ClientManager clientManager;

    @Inject
    public WikiCommand(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public String getName() {
        return "wiki";
    }

    @Override
    public String getDescription() {
        return "View the wiki";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        new WikiMenu(player, client).show(player);
    }


}
