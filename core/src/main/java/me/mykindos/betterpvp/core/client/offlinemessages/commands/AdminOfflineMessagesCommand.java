package me.mykindos.betterpvp.core.client.offlinemessages.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.offlinemessages.OfflineMessagesHandler;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.Player;

import java.util.Arrays;

@Singleton
public class AdminOfflineMessagesCommand extends Command {

    private final OfflineMessagesHandler offlineMessagesHandler;
    private final ClientManager clientManager;

    @Inject
    public AdminOfflineMessagesCommand(OfflineMessagesHandler offlineMessagesHandler, ClientManager clientManager) {
        this.offlineMessagesHandler = offlineMessagesHandler;
        this.clientManager = clientManager;
    }

    @Override
    public String getName() {
        return "adminofflinemessages";
    }

    @Override
    public String getDescription() {
        return "core.command.admin-offline-messages.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 3) {
            UtilMessage.message(player, "core.prefix.offlinemessages", "core.command.offlinemessages.admin.usage");
            return;
        }

        clientManager.search().offline(args[0]).thenAcceptAsync((targetOptional) -> {
            if (targetOptional.isEmpty()) {
                UtilMessage.message(player, "core.prefix.core", "core.command.balance.player_not_found",
                        net.kyori.adventure.text.Component.text(args[0], net.kyori.adventure.text.format.NamedTextColor.YELLOW));
                return;
            }
            Client target = targetOptional.get();
            offlineMessagesHandler.showMenuForMessagesForClientAfterTime(player, target, UtilTime.parseTimeString(Arrays.copyOfRange(args, 1, 3)));
        });

    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
