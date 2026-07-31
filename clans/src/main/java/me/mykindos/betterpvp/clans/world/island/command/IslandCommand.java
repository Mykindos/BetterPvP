package me.mykindos.betterpvp.clans.world.island.command;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Root {@code /island} command for admin discovery-island tooling. Holds the {@code create}, {@code list},
 * {@code tp}, {@code delete} and {@code pool} subcommands. Defaults to the ADMIN rank via the command loader config.
 */
@Singleton
public class IslandCommand extends Command {

    @Override
    public String getName() {
        return "island";
    }

    @Override
    public String getDescription() {
        return "Discovery island instance management";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.simpleMessage(player, "Islands", Component.text("Usage: ", NamedTextColor.YELLOW)
                .append(Component.text("/island <create|list|tp|delete|pool>")));
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.SUBCOMMAND.name() : ArgumentType.NONE.name();
    }

}
