package me.mykindos.betterpvp.core.framework.server.network;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the Velocity network player count from a backend server using the BungeeCord-compatible
 * plugin messaging channel.
 * <p>
 * Messages must be sent through a player's active proxy connection,
 * so <b>this service cannot refresh while the backend has zero online players</b>.
 */
@Singleton
@BPvPListener
public class PluginMessagingNetworkPlayerCountService implements NetworkPlayerCountService, Listener, PluginMessageListener {

    private static final String CHANNEL = "BungeeCord";
    private static final String GET_SERVERS = "GetServers";
    private static final String PLAYER_COUNT = "PlayerCount";

    private final Core core;
    private final AtomicInteger localPlayerCount = new AtomicInteger();
    private final Map<String, Integer> playerCounts = new ConcurrentHashMap<>();

    @Config(path = "network.player-count.request-interval-ticks", defaultValue = "100")
    private long requestIntervalTicks;

    @Inject
    private PluginMessagingNetworkPlayerCountService(Core core) {
        this.core = core;
    }

    @Override
    public int getOnlineCount() {
        final int networkCount = playerCounts.values().stream().mapToInt(Integer::intValue).sum();
        return Math.max(localPlayerCount.get(), networkCount);
    }

    @Override
    public Map<String, Integer> getServerPlayerCounts() {
        return Collections.unmodifiableMap(playerCounts);
    }

    @EventHandler
    public void onServerStart(ServerStartEvent event) {
        this.localPlayerCount.set(Bukkit.getOnlinePlayers().size());
        this.playerCounts.clear();

        Bukkit.getMessenger().registerOutgoingPluginChannel(core, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(core, CHANNEL, this);

        UtilServer.runTaskTimer(core, this::requestUpdate, 40L, requestIntervalTicks);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.localPlayerCount.incrementAndGet();
        UtilServer.runTaskLater(core, this::requestUpdate, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.localPlayerCount.updateAndGet(count -> Math.max(0, count - 1));
    }

    @Override
    public void onPluginMessageReceived(@NotNull String incomingChannel, @NotNull Player player, byte[] message) {
        if (!CHANNEL.equals(incomingChannel)) {
            return;
        }

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message))) {
            switch (input.readUTF()) {
                case GET_SERVERS -> updateServers(input.readUTF());
                case PLAYER_COUNT -> {
                    final String serverName = input.readUTF().toLowerCase();
                    this.playerCounts.put(serverName, Math.max(0, input.readInt()));
                }
                default -> {
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void requestUpdate() {
        final Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) {
            this.playerCounts.clear();
            return;
        }

        if (this.playerCounts.isEmpty()) {
            sendMessage(carrier, GET_SERVERS, null);
            return;
        }

        this.playerCounts.keySet().forEach(serverName -> sendMessage(carrier, PLAYER_COUNT, serverName));
    }

    private void updateServers(String servers) {
        for (String serverName : servers.split(",\\s*")) {
            if (!serverName.isBlank()) {
                this.playerCounts.putIfAbsent(serverName.toLowerCase(), 0);
            }
        }
    }

    private void sendMessage(Player carrier, String subchannel, String argument) {
        try (ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
             DataOutputStream output = new DataOutputStream(outputBytes)) {
            output.writeUTF(subchannel);
            if (argument != null) {
                output.writeUTF(argument);
            }
            carrier.sendPluginMessage(core, CHANNEL, outputBytes.toByteArray());
        } catch (IOException ignored) {
        }
    }

}
