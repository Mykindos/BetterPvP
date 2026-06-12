package me.mykindos.betterpvp.hub.commands.queue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import me.mykindos.betterpvp.orchestration.model.QueueSnapshot;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(QueueCommand.class)
public class QueueViewCommand extends Command {

    private final OrchestrationGateway orchestrationGateway;

    @Inject
    public QueueViewCommand(OrchestrationGateway orchestrationGateway) {
        this.orchestrationGateway = orchestrationGateway;
    }

    @Override
    public String getName() {
        return "view";
    }

    @Override
    public String getDescription() {
        return "hub.command.queue-view.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "core.prefix.queue", "hub.queue.view.usage");
            return;
        }

        final String serverName = args[0];
        try {
            final QueueSnapshot snapshot = orchestrationGateway.getQueueSnapshot(QueueCommandSupport.buildTarget(serverName)).join();
            UtilMessage.message(player, "core.prefix.queue", "hub.queue.view.snapshot-header");
            UtilMessage.message(player, QueueCommandSupport.buildQueueSnapshotMessage(snapshot));
        } catch (Exception ex) {
            QueueCommandSupport.logCommandFailure("load queue snapshot for " + serverName, ex);
            UtilMessage.message(player, "core.prefix.queue", "hub.queue.view.failed");
        }
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }
}
