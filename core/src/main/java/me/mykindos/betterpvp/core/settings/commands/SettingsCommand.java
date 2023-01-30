package me.mykindos.betterpvp.core.settings.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.menu.MenuManager;
import me.mykindos.betterpvp.core.settings.menus.SettingsMenu;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@Singleton
public class SettingsCommand extends Command {

    private final MenuManager menuManager;

    @Inject
    public SettingsCommand(MenuManager menuManager){
        this.menuManager = menuManager;
    }

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
        menuManager.openMenu(player, new SettingsMenu(player, 9, ChatColor.GREEN.toString() + ChatColor.BOLD + "Settings"));
    }


}
