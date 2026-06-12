package me.mykindos.betterpvp.hub.commands.queue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.hub.feature.queue.HubQueueStatusRegistry;
import me.mykindos.betterpvp.orchestration.model.QueueStatusUpdate;
import org.bukkit.entity.Player;

@Singleton
public class QueueCommand extends Command {

    private final HubQueueStatusRegistry queueStatusRegistry;

    @Inject
    public QueueCommand(HubQueueStatusRegistry queueStatusRegistry) {
        this.queueStatusRegistry = queueStatusRegistry;
    }

    @Override
    public String getName() {
        return "queue";
    }

    @Override
    public String getDescription() {
        return "hub.command.queue.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length > 0) {
            UtilMessage.message(player, "core.prefix.queue", "hub.queue.usage-header");
            UtilMessage.message(player, QueueCommandSupport.buildUsageMessage(client));
            return;
        }

        final QueueStatusUpdate status = queueStatusRegistry.getStatus(player.getUniqueId()).orElse(null);
        if (status == null) {
            UtilMessage.message(player, "core.prefix.queue", "hub.queue.not-queued-any");
            return;
        }

        UtilMessage.message(player, "core.prefix.queue", "hub.queue.status-header");
        UtilMessage.message(player, QueueCommandSupport.buildQueueMessage(status));
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.PLAYER;
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.SUBCOMMAND.name() : ArgumentType.NONE.name();
    }
}
