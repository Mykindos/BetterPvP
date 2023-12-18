package me.mykindos.betterpvp.core.command.commands.qol;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
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
            if (args[0].startsWith("~")) {
                args[0] = args[0].substring(1);
                x = player.getX();
            }
            try {
                x += Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                x += 0;
            }
            if (args[1].startsWith("~")) {
                args[1] = args[1].substring(1);
                y = player.getY();
            }
            try {
                y += Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                y +=0;
            }

            if (args[2].startsWith("~")) {
                args[2] = args[2].substring(1);
                z = player.getZ();
            }
            try {
                z += Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                z += 0;
            }

            player.teleport(new Location(player.getWorld(), x, y, z));
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
