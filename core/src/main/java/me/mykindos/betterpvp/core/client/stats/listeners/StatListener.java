package me.mykindos.betterpvp.core.client.stats.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.events.StatPropertyUpdateEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Set;

@BPvPListener
@Singleton
@CustomLog
public class StatListener implements Listener {

    @Config(path = "stats.enabled", defaultValue = "true")
    @Inject
    private boolean statsEnabled;

    private final ClientManager clientManager;

    @Inject
    public StatListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    /**
     * Global stats feature flag: cancels further processing of any stat-change event when stats are disabled.
     * Runs at LOWEST priority so other handlers skip the event if it gets cancelled here.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStatUpdate(StatPropertyUpdateEvent event) {
        if (!statsEnabled) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(ClientQuitEvent event) {
        log.debug("process stats on quit").submit();
        clientManager.getSqlLayer().processStatUpdates(Set.of(event.getClient()), Core.getCurrentRealm());
    }
}
