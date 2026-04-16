package me.mykindos.betterpvp.hub.commands.queue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import me.mykindos.betterpvp.orchestration.model.QueueState;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(QueueCommand.class)
public class QueuePauseCommand extends Command {

    private final OrchestrationGateway orchestrationGateway;

    @Inject
    public QueuePauseCommand(OrchestrationGateway orchestrationGateway) {
        this.orchestrationGateway = orchestrationGateway;
    }

    @Override
    public String getName() {
        return "pause";
    }

    @Override
    public String getDescription() {
        return "Pause new queue admissions for a server";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.simpleMessage(player, "Queue", "<red>Usage: /queue pause <server>");
            return;
        }

        final String serverName = args[0];
        try {
            orchestrationGateway.setQueueState(QueueCommandSupport.buildTarget(serverName), QueueState.PAUSED).join();
            UtilMessage.simpleMessage(player, "Queue", "Set queue state for <yellow>" + serverName + "</yellow> to <yellow>PAUSED");
        } catch (Exception ex) {
            QueueCommandSupport.logCommandFailure("update queue state for " + serverName + " to PAUSED", ex);
            UtilMessage.simpleMessage(player, "Queue", "<red>Failed to update queue state.");
        }
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }
}
