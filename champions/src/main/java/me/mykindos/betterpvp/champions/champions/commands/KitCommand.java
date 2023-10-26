package me.mykindos.betterpvp.champions.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.commands.menu.KitMenu;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.menu.MenuManager;
import org.bukkit.entity.Player;

@Singleton
public class KitCommand extends Command {

    @Inject
    RoleManager roleManager;

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
        MenuManager.openMenu(player, new KitMenu(player, roleManager));
    }
}
