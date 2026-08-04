package me.mykindos.betterpvp.clans.world.ship.command;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Root {@code /ship} command for builder tooling on moored vessels. Holds {@code list}, {@code disable} and
 * {@code enable} — the way to see the dock underneath a hull without editing the map.
 */
@Singleton
public class ShipCommand extends Command {

    static final String PREFIX = "Ships";

    @Override
    public String getName() {
        return "ship";
    }

    @Override
    public String getDescription() {
        return "Inspect and temporarily un-moor the ships in this world";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.simpleMessage(player, PREFIX, Component.text("Usage: ", NamedTextColor.YELLOW)
                .append(Component.text("/ship <list|disable|enable>")));
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.SUBCOMMAND.name() : ArgumentType.NONE.name();
    }

}
