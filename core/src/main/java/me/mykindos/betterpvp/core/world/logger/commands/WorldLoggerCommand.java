package me.mykindos.betterpvp.core.world.logger.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;


@Singleton
public class WorldLoggerCommand extends Command {

    @Inject
    public WorldLoggerCommand() {
        aliases.add("wl");
        aliases.add("co");
    }

    @Override
    public String getName() {
        return "worldlogger";
    }

    @Override
    public String getDescription() {
        return "Base command for world logging";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

    }

}
