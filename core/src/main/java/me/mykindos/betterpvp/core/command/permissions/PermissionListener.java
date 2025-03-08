package me.mykindos.betterpvp.core.command.permissions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@CustomLog
@BPvPListener
@Singleton
public class PermissionListener implements Listener {
    private final PermissionManager permissionManager;

    @Inject
    PermissionListener(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    @EventHandler
    public void onClientJoin(ClientJoinEvent event) {
        permissionManager.onJoin(event.getClient(), event.getPlayer());
    }

    @EventHandler
    public void onClientLeave(ClientQuitEvent event) {
       permissionManager.onQuit(event.getPlayer());
    }

}
