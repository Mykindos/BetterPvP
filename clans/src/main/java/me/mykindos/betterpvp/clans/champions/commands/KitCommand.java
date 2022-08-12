package me.mykindos.betterpvp.clans.champions.commands;

import me.mykindos.betterpvp.clans.champions.commands.menu.KitMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.menu.MenuManager;
import org.bukkit.entity.Player;

public class KitCommand extends Command {

    @Override
    public String getName() {
        return "kit";
    }

    @Override
    public String getDescription() {
        return "Equip a kit";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        MenuManager.openMenu(player, new KitMenu(player));
    }
}
