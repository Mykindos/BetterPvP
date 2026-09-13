package me.mykindos.betterpvp.core.framework.net;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Transfer by asking the proxy, which is what a Velocity or BungeeCord network understands.
 * <p>
 * The request rides the player's own connection, so a player can always be moved even when nothing else on the
 * network can be reached.
 */
@BPvPListener
@Singleton
@CustomLog
public class ProxyPlayerTransfer implements PlayerTransfer, Listener {

    private static final String CHANNEL = "BungeeCord";
    private static final String CONNECT = "Connect";

    private final Core core;

    @Inject
    public ProxyPlayerTransfer(@NotNull Core core) {
        this.core = core;
    }

    @EventHandler
    public void onServerStart(@NotNull ServerStartEvent event) {
        Bukkit.getMessenger().registerOutgoingPluginChannel(core, CHANNEL);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> transfer(@NotNull Player traveller, @NotNull String server) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeUTF(CONNECT);
            output.writeUTF(server);
            traveller.sendPluginMessage(core, CHANNEL, bytes.toByteArray());
            return CompletableFuture.completedFuture(true);
        } catch (IOException exception) {
            log.error("Could not send {} to server '{}'", traveller.getName(), server, exception).submit();
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
