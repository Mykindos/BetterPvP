package me.mykindos.betterpvp.core.command.commands.qol;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.menu.MenuManager;
import me.mykindos.betterpvp.core.menu.demo.DemoMenu;
import org.bukkit.entity.Player;

public class TestMenuCommand extends Command {


    @Override
    public String getName() {
        return "test";
    }

    @Override
    public String getDescription() {
        return "test if you dare";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        MenuManager.openMenu(player, new DemoMenu(player));
    }
}
