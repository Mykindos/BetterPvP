package me.mykindos.betterpvp.core.client.stats.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Set;

@BPvPListener
@Singleton
@CustomLog
public class StatListener implements Listener {
    private final ClientManager clientManager;

    @Inject
    public StatListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(ClientQuitEvent event) {
        log.debug("process stats on quit").submit();
        clientManager.getSqlLayer().processStatUpdates(Set.of(event.getClient()), Core.getCurrentRealm());
    }
}
