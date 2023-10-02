package me.mykindos.betterpvp.progression.tree.fishing.commands;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class FishingCommand extends Command {

    @Override
    public String getName() {
        return "fishing";
    }

    @Override
    public String getDescription() {
        return "Fishing command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 0) return;

        UtilMessage.simpleMessage(player, "Fishing", "Fishing commands:");
        UtilMessage.simpleMessage(player, "Fishing", "None");
    }
}
