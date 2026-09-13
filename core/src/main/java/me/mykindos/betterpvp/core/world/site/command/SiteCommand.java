package me.mykindos.betterpvp.core.world.site.command;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Admin tooling for sites and their live instances. Defaults to the ADMIN rank via the command loader config.
 */
@Singleton
public class SiteCommand extends Command {

    @Override
    public String getName() {
        return "site";
    }

    @Override
    public String getDescription() {
        return "Site and instance management";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        UtilMessage.simpleMessage(player, "Sites", Component.text("Usage: ", NamedTextColor.YELLOW)
                .append(Component.text("/site <list|create|tp|delete>")));
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.SUBCOMMAND.name() : ArgumentType.NONE.name();
    }
}
