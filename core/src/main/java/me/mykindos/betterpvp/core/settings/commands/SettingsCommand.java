package me.mykindos.betterpvp.core.settings.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.settings.menus.SettingsMenu;
import org.bukkit.entity.Player;

@Singleton
public class SettingsCommand extends Command {

    @Inject
    private ClientManager clientManager;

    @Override
    public String getName() {
        return "settings";
    }

    @Override
    public String getDescription() {
        return "View or modify your settings";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        new SettingsMenu(player, client).show(player);
    }


}
