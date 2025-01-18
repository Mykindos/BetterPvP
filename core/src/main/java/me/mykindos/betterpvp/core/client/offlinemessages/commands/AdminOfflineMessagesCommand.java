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
        return "Retrieve your offline messages from the past time";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 3) {
            UtilMessage.message(player, "OfflineMessages", "Usage: <yellow>/adminofflinemessages <green><player> <number> <unit></green></yellow>");
            return;
        }

        clientManager.search().offline(args[0], targetOptional -> {
            if (targetOptional.isEmpty()) {
                UtilMessage.message(player, "Core", "<yellow>%s</yellow> is not a valid player name", args[0]);
                return;
            }
            Client target = targetOptional.get();
            offlineMessagesHandler.showMenuForMessagesForClientAfterTime(player, target.getName(), target.getUniqueId(), UtilTime.parseTimeString(Arrays.copyOfRange(args, 1, 3)));
        }, true);

    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
