package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
public class KickCommand extends Command implements IConsoleCommand {


    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public String getDescription() {
        return "Kick a player from the server";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        execute(player, args);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 2) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                String reason = args[1];
                target.kick(UtilMessage.deserialize("<red>[Kick] <gray>" + reason));
                UtilMessage.simpleBroadcast("Kick", "<alt2>%s</alt2> kicked <alt2>%s</alt2> for <alt>%s</alt>", sender.getName(), target.getName(), reason);
            }
        } else {
            UtilMessage.message(sender, "Command", "You must specify a player and a reason");
        }
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Override
    public String getArgumentType(int argCount) {
        if(argCount == 1){
            return ArgumentType.PLAYER.name();
        }

        return ArgumentType.NONE.name();
    }



}
