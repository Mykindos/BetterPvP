package me.mykindos.betterpvp.core.client.gamer.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@BPvPListener
@Singleton
public class GamerBossBarListener implements Listener {

    private final ClientManager manager;

    @Inject
    public GamerBossBarListener(ClientManager manager) {
        this.manager = manager;
    }

    // Default priority so this runs before ClientListener marks the client offline at MONITOR.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final Gamer gamer = this.manager.search().online(player).getGamer();
        gamer.getBossBarOverlay().hide(player);
        gamer.getBossBarQueue().hide(player);
    }
}
