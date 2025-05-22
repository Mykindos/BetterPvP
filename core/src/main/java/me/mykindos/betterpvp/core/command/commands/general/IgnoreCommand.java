package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
@CustomLog
public class IgnoreCommand extends Command {

    private final ClientManager clientManager;

    @Inject
    public IgnoreCommand(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public String getName() {
        return "ignore";
    }

    @Override
    public String getDescription() {
        return "Ignore private messages from a player";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length >= 1) {

            clientManager.search().offline(args[0]).thenAcceptAsync(targetOptional -> {
                if (targetOptional.isEmpty()) {
                    UtilMessage.message(player, "Ignore", "Cannot find a player with the name <yellow>%s</yellow>", args[0]);
                    return;
                }
                Client target = targetOptional.get();
                if (client.getIgnores().contains(target.getUniqueId())) {
                    //this player is already ignored, unignore them
                    clientManager.removeIgnore(client, target);
                    UtilMessage.message(player, "Ignore", "You have unignored <yellow>%s</yellow>", target.getName());
                    return;
                }
                clientManager.saveIgnore(client, target);
                UtilMessage.message(player, "Ignore", "You have ignored <yellow>%s</yellow>", target.getName());
            });

        } else {
            UtilMessage.simpleMessage(player, "Command", "Usage: /ignore <player>");
        }
    }

    @Override
    public String getArgumentType(int index) {
        return index == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
