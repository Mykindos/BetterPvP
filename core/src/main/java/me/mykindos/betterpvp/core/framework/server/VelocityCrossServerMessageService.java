package me.mykindos.betterpvp.core.framework.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.framework.server.events.ServerMessageReceivedEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
@BPvPListener
@CustomLog
public class VelocityCrossServerMessageService implements CrossServerMessageService, Listener, PluginMessageListener {

    private static final String CHANNEL = "BungeeCord";
    private static final String FORWARD = "Forward";
    private static final String PLAYER_LIST = "PlayerList";
    private static final String NETWORK_TARGET = "ALL";
    private static final String SERVER_MESSAGE_SUBCHANNEL = "BetterPvP:ServerMessage";
    private static final String EVENT_CHANNEL = "BetterPvP";

    private final Core core;
    private final Set<CompletableFuture<Set<String>>> pendingPlayerListRequests = ConcurrentHashMap.newKeySet();

    @Inject
    public VelocityCrossServerMessageService(Core core) {
        this.core = core;
    }

    @EventHandler
    public void onServerStart(ServerStartEvent event) {
        Bukkit.getMessenger().registerOutgoingPluginChannel(core, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(core, CHANNEL, this);
    }

    @Override
    public void broadcast(ServerMessage message) {
        final ServerMessage outgoing = withServer(message);
        UtilServer.callEventAsync(core, new ServerMessageReceivedEvent(EVENT_CHANNEL, outgoing));
        UtilServer.runTask(core, () -> sendForward(outgoing));
    }

    @Override
    public CompletableFuture<Boolean> isPlayerOnline(String playerName) {
        final CompletableFuture<Boolean> result = new CompletableFuture<>();
        UtilServer.runTask(core, () -> {
            if (playerName == null || playerName.isBlank()) {
                result.complete(false);
                return;
            }

            if (Bukkit.getPlayerExact(playerName) != null) {
                result.complete(true);
                return;
            }

            final Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
            if (carrier == null) {
                result.complete(false);
                return;
            }

            final CompletableFuture<Set<String>> playerListFuture = new CompletableFuture<>();
            pendingPlayerListRequests.add(playerListFuture);

            playerListFuture.whenComplete((players, throwable) -> pendingPlayerListRequests.remove(playerListFuture));
            playerListFuture.thenApply(players ->
                    players.stream().anyMatch(onlineName -> onlineName.equalsIgnoreCase(playerName)))
                    .whenComplete((isOnline, throwable) -> {
                        if (throwable != null) {
                            result.completeExceptionally(throwable);
                            return;
                        }

                        result.complete(isOnline);
                    });

            UtilServer.runTaskLater(core, () -> playerListFuture.complete(Set.of()), 100L);
            sendPlayerListRequest(carrier);
        });
        return result;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String incomingChannel, @NotNull Player player, byte[] message) {
        if (!CHANNEL.equals(incomingChannel)) {
            return;
        }

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message))) {
            final String subchannel = input.readUTF();
            switch (subchannel) {
                case SERVER_MESSAGE_SUBCHANNEL -> receiveForwardedServerMessage(input);
                case PLAYER_LIST -> completePlayerListRequests(input);
                default -> {
                }
            }
        } catch (IOException ex) {
            log.error("Failed to process cross-server plugin message", ex).submit();
        }
    }

    private void receiveForwardedServerMessage(DataInputStream input) throws IOException {
        final byte[] payload = readPayload(input);
        final ServerMessage serverMessage = deserialize(payload);
        UtilServer.callEventAsync(core, new ServerMessageReceivedEvent(EVENT_CHANNEL, serverMessage));
    }

    private void completePlayerListRequests(DataInputStream input) throws IOException {
        final String requestedServer = input.readUTF();
        if (!NETWORK_TARGET.equalsIgnoreCase(requestedServer)) {
            return;
        }

        final String rawPlayerList = input.readUTF();
        final Set<String> players = Arrays.stream(rawPlayerList.split(",\\s*"))
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet());
        pendingPlayerListRequests.forEach(future -> future.complete(players));
    }

    private void sendForward(ServerMessage message) {
        final Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) {
            return;
        }

        final byte[] payload = serialize(message);
        if (payload.length == 0) {
            return;
        }

        try (ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
             DataOutputStream output = new DataOutputStream(outputBytes)) {
            output.writeUTF(FORWARD);
            output.writeUTF(NETWORK_TARGET);
            output.writeUTF(SERVER_MESSAGE_SUBCHANNEL);
            output.writeShort(payload.length);
            output.write(payload);
            carrier.sendPluginMessage(core, CHANNEL, outputBytes.toByteArray());
        } catch (IOException ex) {
            log.error("Failed to forward cross-server message", ex).submit();
        }
    }

    private void sendPlayerListRequest(Player carrier) {
        try (ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
             DataOutputStream output = new DataOutputStream(outputBytes)) {
            output.writeUTF(PLAYER_LIST);
            output.writeUTF(NETWORK_TARGET);
            carrier.sendPluginMessage(core, CHANNEL, outputBytes.toByteArray());
        } catch (IOException ex) {
            log.error("Failed requesting network player list", ex).submit();
        }
    }

    private ServerMessage withServer(ServerMessage message) {
        if (message.getServer() != null && !message.getServer().isBlank()) {
            return message;
        }

        return ServerMessage.builder()
                .channel(message.getChannel())
                .server(Core.getCurrentRealm().getServer().getName())
                .message(message.getMessage())
                .metadata(message.getMetadata())
                .build();
    }

    private byte[] serialize(ServerMessage message) {
        try (ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
             DataOutputStream output = new DataOutputStream(outputBytes)) {
            output.writeUTF(nullToEmpty(message.getChannel()));
            output.writeUTF(nullToEmpty(message.getServer()));
            output.writeUTF(nullToEmpty(message.getMessage()));

            final Map<String, String> metadata = message.getMetadata();
            output.writeInt(metadata != null ? metadata.size() : 0);
            if (metadata != null) {
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    output.writeUTF(entry.getKey());
                    output.writeUTF(nullToEmpty(entry.getValue()));
                }
            }

            return outputBytes.toByteArray();
        } catch (IOException ex) {
            log.error("Failed to serialize cross-server message", ex).submit();
            return new byte[0];
        }
    }

    private ServerMessage deserialize(byte[] payload) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            final ServerMessage.ServerMessageBuilder builder = ServerMessage.builder()
                    .channel(emptyToNull(input.readUTF()))
                    .server(emptyToNull(input.readUTF()))
                    .message(emptyToNull(input.readUTF()));

            final int metadataSize = input.readInt();
            for (int i = 0; i < metadataSize; i++) {
                builder.metadata(input.readUTF(), emptyToNull(input.readUTF()));
            }

            return builder.build();
        }
    }

    private byte[] readPayload(DataInputStream input) throws IOException {
        final short length = input.readShort();
        final byte[] payload = new byte[length];
        input.readFully(payload);
        return payload;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
