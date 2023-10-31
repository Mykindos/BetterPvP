package me.mykindos.betterpvp.lunar.listener.impl;

import com.google.inject.Singleton;
import com.lunarclient.apollo.Apollo;
import me.mykindos.betterpvp.core.framework.events.lunar.LunarClientEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@Singleton
@BPvPListener
public class LunarClientLoginListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (Apollo.getPlayerManager().hasSupport(player.getUniqueId())) {
            UtilServer.callEvent(new LunarClientEvent(player, true));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if (Apollo.getPlayerManager().hasSupport(player.getUniqueId())) {
            UtilServer.callEvent(new LunarClientEvent(player, false));
        }
    }
}
