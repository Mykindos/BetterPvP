package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class IgnoreCommand extends Command {

    private static final String IGNORE_PREFIX = "core.prefix.ignore";

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
        return "core.command.ignore.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length >= 1) {

            clientManager.search().offline(args[0]).thenAcceptAsync(targetOptional -> {
                if (targetOptional.isEmpty()) {
                    UtilMessage.message(player, IGNORE_PREFIX, "core.command.ignore.player_not_found",
                            Component.text(args[0], NamedTextColor.YELLOW));
                    return;
                }
                Client target = targetOptional.get();
                if (client.getIgnores().contains(target.getUniqueId())) {
                    //this player is already ignored, unignore them
                    clientManager.removeIgnore(client, target);
                    UtilMessage.message(player, IGNORE_PREFIX, "core.command.ignore.remove.success",
                            Component.text(target.getName(), NamedTextColor.YELLOW));
                    return;
                }
                clientManager.saveIgnore(client, target);
                UtilMessage.message(player, IGNORE_PREFIX, "core.command.ignore.add.success",
                        Component.text(target.getName(), NamedTextColor.YELLOW));
            });

        } else {
            UtilMessage.message(player, COMMAND_PREFIX, "core.command.ignore.usage");
        }
    }

    @Override
    public String getArgumentType(int index) {
        return index == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
