package me.mykindos.betterpvp.core.command.commands.qol;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class SetViewDistanceCommand extends Command {
    @Override
    public String getName() {
        return "setviewdistance";
    }

    @Override
    public String getDescription() {
        return "Set the view distance of your current world";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(args.length == 0) {
            UtilMessage.message(player, "Command", "Usage: /setviewdistance <distance>");
            return;
        }

        try {
            int distance = Integer.parseInt(args[0]);
            if(distance < 3 || distance > 32){
                UtilMessage.message(player, "Command", "View distance must be between 3 and 32");
                return;
            }

            player.getWorld().setViewDistance(distance);
            player.getWorld().setSimulationDistance(distance);
            UtilMessage.message(player, "Command", "Set view distance to " + distance);
        } catch (NumberFormatException e) {
            UtilMessage.message(player, "Command", "Invalid number");
        }
    }
}
