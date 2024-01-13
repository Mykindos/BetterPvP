package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Singleton
public class InvSeeCommand extends Command {

    public InvSeeCommand() {
        aliases.add("openinv");
    }

    @Override
    public String getName() {
        return "invsee";
    }

    @Override
    public String getDescription() {
        return "Opens a target players inventory";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.simpleMessage(player, "Usage: /invsee <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            player.openInventory(target.getInventory());
            UtilMessage.simpleMessage(player, "You opened <yellow>" + target.getName() + "'s <gray>inventory.");
        } else {
            UtilMessage.simpleMessage(player, "Could not find player <yellow>" + args[0]);
        }

    }
    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
