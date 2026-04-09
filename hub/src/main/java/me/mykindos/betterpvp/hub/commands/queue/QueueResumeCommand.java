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
public class QueueResumeCommand extends Command {

    private final OrchestrationGateway orchestrationGateway;

    @Inject
    public QueueResumeCommand(OrchestrationGateway orchestrationGateway) {
        this.orchestrationGateway = orchestrationGateway;
    }

    @Override
    public String getName() {
        return "resume";
    }

    @Override
    public String getDescription() {
        return "Re-open queue admissions for a server";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.simpleMessage(player, "Queue", "<red>Usage: /queue resume <server>");
            return;
        }

        final String serverName = args[0];
        try {
            orchestrationGateway.setQueueState(QueueCommandSupport.buildClansTarget(serverName), QueueState.OPEN).join();
            UtilMessage.simpleMessage(player, "Queue", "Set queue state for <yellow>" + serverName + "</yellow> to <yellow>OPEN");
        } catch (Exception ex) {
            QueueCommandSupport.logCommandFailure("update queue state for " + serverName + " to OPEN", ex);
            UtilMessage.simpleMessage(player, "Queue", "<red>Failed to update queue state.");
        }
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }
}
