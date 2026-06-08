package me.mykindos.betterpvp.core.client.offlinemessages.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.offlinemessages.OfflineMessagesHandler;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.Player;

@Singleton
public class OfflineMessagesCommand extends Command {

    private final OfflineMessagesHandler offlineMessagesHandler;

    @Inject
    public OfflineMessagesCommand(OfflineMessagesHandler offlineMessagesHandler) {
        this.offlineMessagesHandler = offlineMessagesHandler;
        aliases.add("om");
    }

    @Override
    public String getName() {
        return "offlinemessages";
    }

    @Override
    public String getDescription() {
        return "core.command.offline-messages.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, "core.prefix.offlinemessages", "core.command.offlinemessages.usage");
            return;
        }
        offlineMessagesHandler.showMenuForMessagesForClientAfterTime(player, client, UtilTime.parseTimeString(args));

    }
}
