package me.mykindos.betterpvp.core.command.commands.qol;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Singleton
public class TeleportPositionCommand extends Command {

    @WithReflection
    public TeleportPositionCommand() {
        aliases.add("tppos");
    }

    @Override
    public String getName() {
        return "teleportposition";
    }

    @Override
    public String getDescription() {
        return "Teleport to a specific position";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        double x = 0, y = 0, z = 0;
        if (args.length == 3) {
            Location location = UtilLocation.getTeleportLocation(player.getLocation(), args);
            player.teleport(location);
            UtilMessage.message(player, "Teleport", UtilMessage.deserialize("You teleported to (<green>%s</green>, <green>%s</green>, <green>%s</green>)", location.getX(), location.getY(), location.getZ()));
        } else {
            UtilMessage.message(player, "Teleport", "Correct usage: /tppos x y z");
        }
    }

    @Override
    public String getArgumentType(int arg) {
        ArgumentType argType = switch (arg) {
            case 1 -> ArgumentType.POSITION_X;
            case 2 -> ArgumentType.POSITION_Y;
            case 3 -> ArgumentType.POSITION_Z;
            default -> ArgumentType.NONE;
        };

        return argType.name();
    }
}
