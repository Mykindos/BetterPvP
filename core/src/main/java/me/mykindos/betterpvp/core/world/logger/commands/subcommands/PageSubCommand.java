package me.mykindos.betterpvp.core.world.logger.commands.subcommands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.logger.WorldLogHandler;
import me.mykindos.betterpvp.core.world.logger.WorldLogSession;
import me.mykindos.betterpvp.core.world.logger.commands.WorldLoggerCommand;
import org.bukkit.entity.Player;

@SubCommand(WorldLoggerCommand.class)
public class PageSubCommand extends Command {

    private final WorldLogHandler worldLogHandler;

    @Inject
    public PageSubCommand(WorldLogHandler worldLogHandler) {
        this.worldLogHandler = worldLogHandler;
    }

    @Override
    public String getName() {
        return "page";
    }

    @Override
    public String getDescription() {
        return "View results of a specific page in your session";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        WorldLogSession session = worldLogHandler.getSession(player.getUniqueId());

        try {
            int page = Integer.parseInt(args[0]);
            if (page > session.getPages()) {
                UtilMessage.message(player, "World Logger", "Invalid page number.");
                return;
            }

            session.setCurrentPage(page);
            worldLogHandler.displayResults(player, session, page);
        } catch (NumberFormatException ex) {
            UtilMessage.message(player, "World Logger", "Invalid page number.");
        }
    }
}
