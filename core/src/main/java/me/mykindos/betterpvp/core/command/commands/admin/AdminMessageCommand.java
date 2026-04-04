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

import java.util.Arrays;
import java.util.Collection;

@Singleton
public class AdminMessageCommand extends Command {

    private final Core core;
    private final ClientManager clientManager;
    private final CrossServerMessageService crossServerMessageService;

    @Inject
    public AdminMessageCommand(Core core, ClientManager clientManager, CrossServerMessageService crossServerMessageService) {
        this.core = core;
        this.clientManager = clientManager;
        this.crossServerMessageService = crossServerMessageService;

        aliases.add("ma");
    }

    @Override
    public String getName() {
        return "messageadmin";
    }

    @Override
    public String getDescription() {
        return "Send an admin message to a player";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        final Gamer gamer = client.getGamer();

        if (args.length == 0) {
            UtilMessage.message(player, "Core", "Usage: <player> <message>");
            return;
        }

        if (args.length == 1) {
            UtilMessage.message(player, "Core", "You must specify a message");
            return;
        }

        final Collection<Client> matches = clientManager.search(player).advancedOnline(args[0]);
        if (matches.size() > 1) {
            return;
        }

        if (matches.size() == 1) {
            sendMessage(player, gamer, matches.iterator().next(), args);
            return;
        }

        clientManager.search(player).offline(args[0]).thenAccept(targetOptional -> {
            if (targetOptional.isEmpty()) {
                return;
            }

            final Client target = targetOptional.get();
            crossServerMessageService.isPlayerOnline(target.getName()).thenAccept(isOnline ->
                    UtilServer.runTask(core, () -> {
                        if (!isOnline) {
                            UtilMessage.message(player, "Core", "<gray>No online player to message found");
                            return;
                        }

                        sendMessage(player, gamer, target, args);
                    }));
        });
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.HELPER;
    }

    @Override
    public String getArgumentType(int argCount) {
        if (argCount == 1) {
            return ArgumentType.PLAYER.name();
        }
        return ArgumentType.NONE.name();
    }

    private void sendMessage(Player player, Gamer gamer, Client receiver, String... args) {
        gamer.setLastAdminMessenger(receiver.getUuid());

        final ServerMessage message = ServerMessage.builder()
                .channel("AdminDirectMessage")
                .message(String.join(" ", Arrays.copyOfRange(args, 1, args.length)))
                .metadata("sender", player.getUniqueId().toString())
                .metadata("target", receiver.getUniqueId().toString())
                .build();
        crossServerMessageService.broadcast(message);
    }
}
