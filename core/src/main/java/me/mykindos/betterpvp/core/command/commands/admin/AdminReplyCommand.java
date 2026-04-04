package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.server.CrossServerMessageService;
import me.mykindos.betterpvp.core.framework.server.ServerMessage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.UUID;

@Singleton
public class AdminReplyCommand extends Command {

    private final Core core;
    private final ClientManager clientManager;
    private final CrossServerMessageService crossServerMessageService;

    @Inject
    public AdminReplyCommand(Core core, ClientManager clientManager, CrossServerMessageService crossServerMessageService) {
        this.core = core;
        this.clientManager = clientManager;
        this.crossServerMessageService = crossServerMessageService;

        aliases.add("ra");
    }

    @Override
    public String getName() {
        return "replyadmin";
    }

    @Override
    public String getDescription() {
        return "Send an admin message to a player";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Gamer gamer = client.getGamer();
        if (args.length == 0) {
            UtilMessage.message(player, "Core", "You must specify a message");
            return;
        }
        if (gamer.getLastAdminMessenger() == null) {
            UtilMessage.message(player, "Core", UtilMessage.deserialize("<gray>No previous player to reply to"));
            return;
        }

        clientManager.search(player).offline(UUID.fromString(gamer.getLastAdminMessenger())).thenAccept(optionalReceiver -> {
            if (optionalReceiver.isEmpty()) {
                UtilServer.runTask(core, () ->
                        UtilMessage.message(player, "Core", UtilMessage.deserialize("<gray>No online player to reply to found")));
                return;
            }

            final Client receivingClient = optionalReceiver.get();
            crossServerMessageService.isPlayerOnline(receivingClient.getName()).thenAccept(isOnline ->
                    UtilServer.runTask(core, () -> {
                        if (!isOnline) {
                            UtilMessage.message(player, "Core", UtilMessage.deserialize("<gray>No online player to reply to found"));
                            return;
                        }

                        gamer.setLastAdminMessenger(receivingClient.getUuid());
                        final ServerMessage message = ServerMessage.builder()
                                .channel("AdminDirectMessage")
                                .message(String.join(" ", args))
                                .metadata("sender", player.getUniqueId().toString())
                                .metadata("target", receivingClient.getUniqueId().toString())
                                .build();
                        crossServerMessageService.broadcast(message);
                    }));
        });
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.HELPER;
    }
}
