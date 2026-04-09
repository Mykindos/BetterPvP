package me.mykindos.betterpvp.hub.commands.queue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.hub.feature.queue.HubQueueStatusRegistry;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(QueueCommand.class)
public class QueueLeaveCommand extends Command {

    private final OrchestrationGateway orchestrationGateway;
    private final HubQueueStatusRegistry queueStatusRegistry;

    @Inject
    public QueueLeaveCommand(OrchestrationGateway orchestrationGateway, HubQueueStatusRegistry queueStatusRegistry) {
        this.orchestrationGateway = orchestrationGateway;
        this.queueStatusRegistry = queueStatusRegistry;
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getDescription() {
        return "Leave the queue you are currently in";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (queueStatusRegistry.getStatus(player.getUniqueId()).isEmpty()) {
            UtilMessage.simpleMessage(player, "Queue", "<gray>You are not currently queued for any server.");
            return;
        }

        try {
            orchestrationGateway.leaveQueue(player.getUniqueId()).join();
            UtilMessage.simpleMessage(player, "Queue", "<gray>You have left the queue.");
        } catch (Exception ex) {
            QueueCommandSupport.logCommandFailure("leave queue for " + player.getName(), ex);
            UtilMessage.simpleMessage(player, "Queue", "<red>Failed to leave the queue. Please try again.");
        }
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.PLAYER;
    }

    @Override
    public String getArgumentType(int argCount) {
        return ArgumentType.NONE.name();
    }
}
