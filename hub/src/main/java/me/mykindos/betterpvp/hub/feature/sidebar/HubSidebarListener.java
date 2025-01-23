package me.mykindos.betterpvp.hub.feature.sidebar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.framework.sidebar.Sidebar;
import me.mykindos.betterpvp.core.framework.sidebar.SidebarController;
import me.mykindos.betterpvp.core.framework.sidebar.SidebarType;
import me.mykindos.betterpvp.core.framework.sidebar.events.SidebarBuildEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class HubSidebarListener implements Listener {

    @Inject
    private final HubSidebarBuilder sidebarBuilder;

    @Inject
    private HubSidebarListener(HubSidebarBuilder sidebarBuilder, SidebarController controller) {
        this.sidebarBuilder = sidebarBuilder;
        controller.setDefaultProvider(gmr -> new Sidebar(gmr, "Mineplex Clans", SidebarType.HUB));
    }

    @EventHandler
    public void onBuild(SidebarBuildEvent event) {
        if (!SidebarType.HUB.equals(event.getSidebarType())) {
            return;
        }

        Gamer gamer = event.getGamer();
        Player player = gamer.getPlayer();
        if (player == null) {
            event.getSidebar().close();
            return;
        }

        this.sidebarBuilder.build(event);
    }
}
