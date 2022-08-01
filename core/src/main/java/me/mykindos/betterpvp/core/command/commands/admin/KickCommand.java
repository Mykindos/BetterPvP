package me.mykindos.betterpvp.core.command.commands.admin;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.types.PlayerArgCommand;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@WithReflection
public class KickCommand extends PlayerArgCommand {


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
        if (args.length == 2) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                String reason = args[1];
                target.kick(Component.text(ChatColor.RED + "[Kick] " + ChatColor.GRAY + reason));
                UtilMessage.broadcast("Kick", ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " kicked " + ChatColor.YELLOW + target.getName() + ChatColor.GRAY + " for "
                        + ChatColor.GREEN + reason);
            }
        } else {
            UtilMessage.message(player, "Command", "You must specify a player and a reason");
        }
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }


}
