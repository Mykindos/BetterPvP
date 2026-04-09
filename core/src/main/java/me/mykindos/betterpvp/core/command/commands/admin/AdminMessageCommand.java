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
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
        final var search = clientManager.search(player);

        if (args.length == 0) {
            UtilMessage.message(player, "Core", "Usage: <player> <message>");
            return;
        }

        if (args.length == 1) {
            UtilMessage.message(player, "Core", "You must specify a message");
            return;
        }

        // Handles zero-matches
        final Collection<Client> matches = search.inform(false).advancedOnline(args[0]);
        if (matches.size() > 1) {
            search.tooManyMatches(matches, args[0]);
            return;
        }

        if (matches.size() == 1) {
            sendMessage(player, gamer, matches.iterator().next(), args);
            return;
        }

        // This is not silent, it will show ZERO MATCHES if not found
        final CompletableFuture<Optional<Client>> offlineSearch = clientManager.search(player).offline(args[0]);

        offlineSearch.thenAccept(targetOptional -> {
            if (targetOptional.isEmpty()) {
                return; // No player found, message was sent above
            }

            // Since the player does exist, let's see if they're online in the network, if they aren't show zero matches
            final Client target = targetOptional.get();
            crossServerMessageService.isPlayerOnline(target.getName()).thenAccept(isOnline ->
                    UtilServer.runTask(core, () -> {
                        if (!isOnline) {
                            search.zeroMatches(args[0]);
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
        new SoundEffect("minecraft", "block.amethyst_block.resonate", 1.0F).play(player);
    }
}
