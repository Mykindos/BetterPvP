package me.mykindos.betterpvp.core.client.stats.listeners;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TimedStatListener implements Listener {
    public static final long UPDATE_TIME = 60_000;
    protected final Map<UUID, Long> lastUpdateMap = new ConcurrentHashMap<>();
    protected final ClientManager clientManager;

    protected TimedStatListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    abstract public void onUpdate(final Client client, final long deltaTime);

    @UpdateEvent(delay = UPDATE_TIME)
    public void doUpdateEvent() {
        new ArrayList<>(Bukkit.getOnlinePlayers()).forEach(
            this::doUpdate
        );
    }

    /**
     * Does the update for the specified player
     * @param player
     */
    public void doUpdate(Player player) {
        final long currentTime = System.currentTimeMillis();
        final long lastUpdate = lastUpdateMap.computeIfAbsent(player.getUniqueId(), k -> currentTime);
        final Client client = clientManager.search().online(player);
        onUpdate(client, currentTime - lastUpdate);
        lastUpdateMap.put(player.getUniqueId(), currentTime);
    }


    @EventHandler
    public void onLoginEvent(ClientJoinEvent event) {
        lastUpdateMap.put(event.getClient().getUniqueId(), System.currentTimeMillis());
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogoutEvent(ClientQuitEvent event) {
        final long lastUpdate = lastUpdateMap.remove(event.getClient().getUniqueId());
        final long currentTime = System.currentTimeMillis();
        onUpdate(event.getClient(), currentTime - lastUpdate);
    }

}
