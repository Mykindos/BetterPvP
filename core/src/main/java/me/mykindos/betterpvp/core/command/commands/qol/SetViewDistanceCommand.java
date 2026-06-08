package me.mykindos.betterpvp.core.command.commands.qol;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class SetViewDistanceCommand extends Command {
    
    @Override
    public String getName() {
        return "setviewdistance";
    }

    @Override
    public String getDescription() {
        return "core.command.set-view-distance.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(args.length == 0) {
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.setviewdistance.usage");
            return;
        }

        try {
            int distance = Integer.parseInt(args[0]);
            if(distance < 3 || distance > 32){
                UtilMessage.message(player, COMMAND_PREFIX, "core.command.setviewdistance.out_of_range");
                return;
            }

            player.getWorld().setViewDistance(distance);
            player.getWorld().setSimulationDistance(distance);
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.setviewdistance.success",
                    Component.text(distance, NamedTextColor.YELLOW));
        } catch (NumberFormatException e) {
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.setviewdistance.invalid_number");
        }
    }
}
