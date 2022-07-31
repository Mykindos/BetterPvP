package me.mykindos.betterpvp.core.command.commands.general;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@WithReflection
public class ListCommand extends Command {

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "get a list of all players online";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        int size = Bukkit.getOnlinePlayers().size();

        List<String> players = new ArrayList<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            players.add(ChatColor.GRAY + " " + onlinePlayer.getName() + ChatColor.YELLOW);
        }

        UtilMessage.message(player, "List", "There are currently " + ChatColor.YELLOW + size + ChatColor.GRAY + " players online.");
        UtilMessage.message(player, "List", ChatColor.YELLOW + players.toString());

    }
}
