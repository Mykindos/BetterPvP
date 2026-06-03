package me.mykindos.betterpvp.core.command.commands.admin.zone;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

/**
 * Root {@code /zone} command for zone tooling. Holds the {@code list} subcommand and the {@code discovery} group; left
 * open for future zone subcommands. Defaults to the ADMIN rank via the command loader config.
 */
@Singleton
public class ZoneCommand extends Command {

    @Override
    public String getName() {
        return "zone";
    }

    @Override
    public String getDescription() {
        return "Zone management and discovery tooling";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.message(player, "Zones", "<yellow>Usage:</yellow> /zone <list|discovery>");
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.SUBCOMMAND.name() : ArgumentType.NONE.name();
    }
}
