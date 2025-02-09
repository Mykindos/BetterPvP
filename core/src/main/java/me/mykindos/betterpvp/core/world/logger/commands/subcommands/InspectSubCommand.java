package me.mykindos.betterpvp.core.world.logger.commands.subcommands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.logger.WorldLogHandler;
import me.mykindos.betterpvp.core.world.logger.commands.WorldLoggerCommand;
import org.bukkit.entity.Player;

@SubCommand(WorldLoggerCommand.class)
public class InspectSubCommand extends Command {

    private final WorldLogHandler worldLogHandler;

    @Inject
    public InspectSubCommand(WorldLogHandler worldLogHandler) {
        this.worldLogHandler = worldLogHandler;
    }

    @Override
    public String getName() {
        return "inspect";
    }

    @Override
    public String getDescription() {
        return "Enable inspection mode";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(worldLogHandler.getInspectingPlayers().remove(player.getUniqueId())){
            UtilMessage.simpleMessage(player, "World Logger", "Inspection mode disabled");
        }else{
            worldLogHandler.getInspectingPlayers().add(player.getUniqueId());
            UtilMessage.simpleMessage(player, "World Logger", "Inspection mode enabled");
        }
    }
}
