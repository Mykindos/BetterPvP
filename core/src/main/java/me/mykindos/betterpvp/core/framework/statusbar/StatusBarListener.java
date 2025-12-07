package me.mykindos.betterpvp.core.framework.statusbar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class StatusBarListener implements Listener {

    private final StatusBarController controller;

    @Inject
    private StatusBarListener(StatusBarController controller) {
        this.controller = controller;
    }

    @EventHandler
    public void onJoin(ClientJoinEvent event) {
        final Gamer gamer = event.getClient().getGamer();
        controller.setup(gamer);
    }
}
