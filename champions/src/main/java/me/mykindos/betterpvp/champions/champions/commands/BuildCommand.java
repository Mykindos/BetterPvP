package me.mykindos.betterpvp.champions.champions.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.menus.ClassSelectionMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;

@Singleton
public class BuildCommand extends Command {

    @Inject
    private ClassSelectionMenu classSelectionMenu;

    @Override
    public String getName() {
        return "build";
    }

    @Override
    public String getDescription() {
        return "Open the build editor";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        classSelectionMenu.show(player);
    }
}
