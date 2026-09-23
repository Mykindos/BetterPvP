package me.mykindos.betterpvp.clans.world.camp.settler.command;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import org.bukkit.entity.Player;

/** Staff tooling for camp settlers, so they can be tested before any way of getting them exists. */
@Singleton
public class SettlerCommand extends Command {

    @Override
    public String getName() {
        return "settler";
    }

    @Override
    public String getDescription() {
        return "clans.command.settler.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        SettlerCommands.send(player, "clans.command.settler.usage");
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.SUBCOMMAND.name() : ArgumentType.NONE.name();
    }
}
