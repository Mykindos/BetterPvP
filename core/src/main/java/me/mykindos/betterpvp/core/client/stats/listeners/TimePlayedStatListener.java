package me.mykindos.betterpvp.core.client.stats.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class TimePlayedStatListener implements Listener {
    //todo conver to periodic stat update (i.e. stats like time, or Minecraft STATISTICS that do not update in the event
    private static final long UPDATE_TIME = 60_000;
    final Map<Player, Long> lastUpdateMap = new WeakHashMap<>();

    private final ClientManager clientManager;

    @Inject
    public TimePlayedStatListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @UpdateEvent(delay = UPDATE_TIME)
    public void onUpdate() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            final long currentTime = System.currentTimeMillis();
            final long lastUpdate = lastUpdateMap.computeIfAbsent(player, k -> currentTime);
            clientManager.search().online(player).getStatContainer().incrementStat(ClientStat.TIME_PLAYED, currentTime - lastUpdate);
            lastUpdateMap.put(player, currentTime);
        });
    }

    @EventHandler
    public void onLogin(ClientJoinEvent event) {
        lastUpdateMap.put(event.getPlayer(), System.currentTimeMillis());
    }


    @EventHandler
    public void onLogout(ClientQuitEvent event) {
        final long lastUpdate = lastUpdateMap.remove(event.getPlayer());
        final long currentTime = System.currentTimeMillis();
        event.getClient().getStatContainer().incrementStat(ClientStat.TIME_PLAYED, currentTime - lastUpdate);
    }
}
