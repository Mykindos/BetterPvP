package me.mykindos.betterpvp.core.settings.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.settings.menus.SettingsMenu;
import org.bukkit.entity.Player;

@Singleton
public class SettingsCommand extends Command {

    @Inject
    private GamerManager gamerManager;

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
        final Gamer gamer = gamerManager.getObject(player.getUniqueId()).orElseThrow();
        new SettingsMenu(player, gamer).show(player);
    }


}
