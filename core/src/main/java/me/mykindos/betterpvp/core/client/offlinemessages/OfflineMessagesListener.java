package me.mykindos.betterpvp.core.client.offlinemessages;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class OfflineMessagesListener implements Listener {
    private final OfflineMessagesHandler offlineMessagesHandler;

    @Inject
    public OfflineMessagesListener(OfflineMessagesHandler offlineMessagesHandler) {
        this.offlineMessagesHandler = offlineMessagesHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClientLogin(ClientJoinEvent event) {
        offlineMessagesHandler.onLogin(event.getClient(), event.getPlayer());

    }



}
