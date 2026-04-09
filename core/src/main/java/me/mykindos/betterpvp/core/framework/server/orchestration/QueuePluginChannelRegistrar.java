package me.mykindos.betterpvp.core.framework.server.orchestration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import static me.mykindos.betterpvp.orchestration.transport.QueuePluginChannels.QUEUE_REQUEST;

@Singleton
@BPvPListener
public class QueuePluginChannelRegistrar implements Listener {

    private final Core core;

    @Inject
    public QueuePluginChannelRegistrar(Core core) {
        this.core = core;
    }

    @EventHandler
    public void onServerStart(ServerStartEvent event) {
        Bukkit.getMessenger().registerOutgoingPluginChannel(core, QUEUE_REQUEST);
    }
}
