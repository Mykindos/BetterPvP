package me.mykindos.betterpvp.core.utilities.model.display;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Singleton
@BPvPListener
public class CooldownBossBarListener implements Listener {

    @Inject
    private CooldownManager cooldownManager;
    @Inject
    private ClientManager clientManager;

    //Start

    //End

    @UpdateEvent(delay = 100)
    public void onTick() {
        this.clientManager.getOnline().forEach(client -> {
            Player player = client.getGamer().getPlayer();
            if(player == null) return;
            this.cooldownManager.updateBossBar(player, client.getGamer());
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onJoin(final PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Client client = clientManager.search().online(player);
        Gamer gamer = client.getGamer();
        gamer.getCooldownComponent().getBossBar().addViewer(player);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onLeave(final PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Client client = clientManager.search().online(player);
        Gamer gamer = client.getGamer();
        gamer.getCooldownComponent().getBossBar().removeViewer(player);
    }

}