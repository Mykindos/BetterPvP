package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Singleton
public class KillCommand extends Command {
    @Override
    public String getName() {
        return "kill";
    }

    @Override
    public String getDescription() {
        return "Kill a player";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            player.setHealth(0);
            UtilMessage.message(player, "Command", "You killed yourself");
        } else {

            if (client.hasRank(Rank.ADMIN)) {
                if (args.length != 1) {
                    UtilMessage.message(player, "Command", "You must specify a player to kill");
                    return;
                }

                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    target.setHealth(0);
                    UtilMessage.message(player, "Command", "You killed " + target.getName());
                }
            }
        }
    }

    @Override
    public String getArgumentType(int argCount) {
        if (argCount == 1) {
            return ArgumentType.PLAYER.name();
        }

        return ArgumentType.NONE.name();
    }
}
