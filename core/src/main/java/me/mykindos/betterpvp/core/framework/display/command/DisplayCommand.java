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
        return "Modify a block display";
    }

    @Override
    public boolean informInsufficientRank() {
        return false;
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 0) return;

        UtilMessage.simpleMessage(player, "Display", "Display commands:");
        UtilMessage.simpleMessage(player, "Display", "/display summon <type> [options] - Summon a display entity");
        UtilMessage.simpleMessage(player, "Display", "/display select - Select a display by punching it");
        UtilMessage.simpleMessage(player, "Display", "/display deselect - Deselect a display");
        UtilMessage.simpleMessage(player, "Display", "/display remove - Despawn the selected display");
        UtilMessage.simpleMessage(player, "Display", "/display transform <action> [options] - Transform the selected display");
    }
}
