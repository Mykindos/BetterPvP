package me.mykindos.betterpvp.core.command.commands.qol;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Singleton
public class TeleportPositionCommand extends Command {

    private static final String TELEPORT_PREFIX = "core.prefix.teleport";

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
        return "core.command.teleport-position.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 3) {
            Location location = UtilLocation.getTeleportLocation(player.getLocation(), args);
            player.teleport(location);
            UtilMessage.message(player, TELEPORT_PREFIX, "core.command.teleportposition.success",
                    Component.text(location.getX(), NamedTextColor.GREEN),
                    Component.text(location.getY(), NamedTextColor.GREEN),
                    Component.text(location.getZ(), NamedTextColor.GREEN));
        } else {
            UtilMessage.message(player, TELEPORT_PREFIX, "core.command.teleportposition.usage");
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
