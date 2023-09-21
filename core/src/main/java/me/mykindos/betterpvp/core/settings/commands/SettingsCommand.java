package me.mykindos.betterpvp.core.settings.commands;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.menu.MenuManager;
import me.mykindos.betterpvp.core.settings.menus.SettingsMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

@Singleton
public class SettingsCommand extends Command {

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
        MenuManager.openMenu(player, new SettingsMenu(player, 9, Component.text("Settings", NamedTextColor.GREEN).decorate(TextDecoration.BOLD)));
    }


}
