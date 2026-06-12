package me.mykindos.betterpvp.core.framework.display.command;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class DisplayCommand extends Command {

    @Override
    public String getName() {
        return "display";
    }

    @Override
    public String getDescription() {
        return "core.command.display.description";
    }

    @Override
    public boolean informInsufficientRank() {
        return false;
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 0) return;

        UtilMessage.message(player, "core.prefix.display", "core.display.help.header");
        UtilMessage.message(player, "core.prefix.display", "core.display.help.summon");
        UtilMessage.message(player, "core.prefix.display", "core.display.help.select");
        UtilMessage.message(player, "core.prefix.display", "core.display.help.deselect");
        UtilMessage.message(player, "core.prefix.display", "core.display.help.remove");
        UtilMessage.message(player, "core.prefix.display", "core.display.help.transform");
    }
}
