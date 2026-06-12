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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(QueueCommand.class)
public class QueueRemoveCommand extends Command {

    private final OrchestrationGateway orchestrationGateway;
    private final ClientManager clientManager;

    @Inject
    public QueueRemoveCommand(OrchestrationGateway orchestrationGateway, ClientManager clientManager) {
        this.orchestrationGateway = orchestrationGateway;
        this.clientManager = clientManager;
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "hub.command.queue-remove.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 1) {
            UtilMessage.message(player, "core.prefix.queue", "hub.queue.remove.usage");
            return;
        }

        final Client target = clientManager.search().offline(args[0]).join().orElse(null);
        if (target == null) {
            UtilMessage.message(player, "core.prefix.queue", "hub.queue.player-not-found");
            return;
        }

        try {
            final boolean removed = orchestrationGateway.removeQueuedPlayer(target.getUniqueId()).join();
            if (!removed) {
                UtilMessage.message(player, "core.prefix.queue", "hub.queue.not-queued",
                        Component.text(target.getName(), NamedTextColor.YELLOW));
                return;
            }

            UtilMessage.message(player, "core.prefix.queue", "hub.queue.remove.success",
                    Component.text(target.getName(), NamedTextColor.YELLOW));
        } catch (Exception ex) {
            QueueCommandSupport.logCommandFailure("remove queued player " + target.getName(), ex);
            UtilMessage.message(player, "core.prefix.queue", "hub.queue.remove.failed");
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
