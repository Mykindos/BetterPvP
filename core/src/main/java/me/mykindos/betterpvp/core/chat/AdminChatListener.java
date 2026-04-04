package me.mykindos.betterpvp.core.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.server.events.ServerMessageReceivedEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@BPvPListener
@Singleton
@CustomLog
public class AdminChatListener implements Listener {

    private final Core core;
    private final ClientManager clientManager;

    @Inject
    public AdminChatListener(Core core, ClientManager clientManager) {
        this.core = core;
        this.clientManager = clientManager;
    }

    @EventHandler
    public void onMessageReceived(ServerMessageReceivedEvent event) {
        final String channel = event.getMessage().getChannel();
        if (channel == null) {
            return;
        }

        if (channel.equalsIgnoreCase("AdminMessage")) {
            handleStaffBroadcast(event);
            return;
        }

        if (channel.equalsIgnoreCase("AdminDirectMessage")) {
            handleDirectMessage(event);
        }
    }

    private void handleStaffBroadcast(ServerMessageReceivedEvent event) {
        final HashMap<String, String> metadata = event.getMessage().getMetadata();
        if (metadata == null) {
            return;
        }

        final String sender = metadata.get("sender");
        if (sender == null) {
            return;
        }

        clientManager.search().offline(UUID.fromString(sender)).thenAccept(clientOptional -> {
            if (clientOptional.isEmpty()) {
                return;
            }

            final Client client = clientOptional.get();
            final String playerName = UtilFormat.spoofNameForLunar(client.getName());
            final Rank sendRank = client.getRank();

            Component senderComponent = Component.empty();
            if (event.getMessage().getServer() != null) {
                senderComponent = Component.text(event.getMessage().getServer() + " ", NamedTextColor.WHITE);
            }
            senderComponent = senderComponent.append(Component.text(playerName, sendRank.getColor())
                    .hoverEvent(HoverEvent.showText(Component.text(sendRank.getName(), sendRank.getColor()))));
            final Component message = Component.text(" " + event.getMessage().getMessage(), NamedTextColor.LIGHT_PURPLE);
            final Component component = Component.empty().append(senderComponent).append(message);

            UtilServer.runTask(core, () -> {
                if (!client.hasRank(Rank.HELPER)) {
                    final Player player = client.getGamer().getPlayer();
                    if (player != null) {
                        UtilMessage.message(player, component);
                        UtilMessage.message(player, "Core", "If a staff member is on this server, they have received this message");
                    }
                }

                clientManager.sendMessageToRank("", component, Rank.HELPER);
            });
        }).exceptionally(ex -> {
            log.error("Failed processing admin message", ex);
            return null;
        });
    }

    private void handleDirectMessage(ServerMessageReceivedEvent event) {
        final HashMap<String, String> metadata = event.getMessage().getMetadata();
        if (metadata == null) {
            return;
        }

        final String sender = metadata.get("sender");
        final String target = metadata.get("target");
        if (sender == null || target == null) {
            return;
        }

        clientManager.search().offline(UUID.fromString(sender)).thenCombine(
                clientManager.search().offline(UUID.fromString(target)),
                (senderOptional, targetOptional) -> {
                    if (senderOptional.isEmpty() || targetOptional.isEmpty()) {
                        return null;
                    }

                    UtilServer.runTask(core, () -> sendDirectAdminMessage(event, senderOptional.get(), targetOptional.get()));
                    return null;
                }
        ).exceptionally(ex -> {
            log.error("Failed processing direct admin message", ex);
            return null;
        });
    }

    private void sendDirectAdminMessage(ServerMessageReceivedEvent event, Client senderClient, Client receiverClient) {
        final String playerName = UtilFormat.spoofNameForLunar(senderClient.getName());
        final Component senderComponent = senderClient.getRank().getPlayerNameMouseOver(playerName);
        final Component receiverComponent = receiverClient.getRank().getPlayerNameMouseOver(receiverClient.getName());
        final Component arrow = Component.text(" -> ", NamedTextColor.DARK_PURPLE);
        final Component message = Component.text(" " + event.getMessage().getMessage(), NamedTextColor.LIGHT_PURPLE);
        final Component component = Component.empty().append(senderComponent).append(arrow).append(receiverComponent).append(message);

        final Optional<Client> onlineReceiver = clientManager.search().online(receiverClient.getUniqueId());
        if (onlineReceiver.isPresent()) {
            final Client loadedReceiver = onlineReceiver.get();
            loadedReceiver.getGamer().setLastAdminMessenger(senderClient.getUuid());

            if (!loadedReceiver.hasRank(Rank.HELPER)) {
                final Player receiverPlayer = loadedReceiver.getGamer().getPlayer();
                if (receiverPlayer != null) {
                    UtilMessage.message(receiverPlayer, component);
                }
            }
        }

        clientManager.sendMessageToRank("", component, Rank.HELPER);
    }
}
