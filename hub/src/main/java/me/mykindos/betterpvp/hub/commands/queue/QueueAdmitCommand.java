package me.mykindos.betterpvp.hub.commands.queue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.orchestration.api.OrchestrationGateway;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(QueueCommand.class)
public class QueueAdmitCommand extends Command {

    private final OrchestrationGateway orchestrationGateway;
    private final ClientManager clientManager;

    @Inject
    public QueueAdmitCommand(OrchestrationGateway orchestrationGateway, ClientManager clientManager) {
        this.orchestrationGateway = orchestrationGateway;
        this.clientManager = clientManager;
    }

    @Override
    public String getName() {
        return "admit";
    }

    @Override
    public String getDescription() {
        return "Force the next admission for a queued player";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.simpleMessage(player, "Queue", "<red>Usage: /queue admit <player>");
            return;
        }

        final Client target = clientManager.search().offline(args[0]).join().orElse(null);
        if (target == null) {
            UtilMessage.simpleMessage(player, "Queue", "<red>Unable to find that player.");
            return;
        }

        try {
            final boolean admitted = orchestrationGateway.admitQueuedPlayer(target.getUniqueId()).join();
            if (!admitted) {
                UtilMessage.simpleMessage(player, "Queue", "<yellow>" + target.getName() + " is not currently queued.");
                return;
            }

            UtilMessage.simpleMessage(player, "Queue", "<green>Marked <yellow>" + target.getName() + "<green> for immediate admission.");
        } catch (Exception ex) {
            QueueCommandSupport.logCommandFailure("admit queued player " + target.getName(), ex);
            UtilMessage.simpleMessage(player, "Queue", "<red>Failed to admit player.");
        }
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.ADMIN;
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 2 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }
}
