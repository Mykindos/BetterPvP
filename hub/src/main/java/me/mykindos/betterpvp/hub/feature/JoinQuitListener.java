package me.mykindos.betterpvp.hub.feature;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class JoinQuitListener implements Listener {

    @Config(path = "spawn", configName = "datapoints")
    @Inject
    private Location location;

    @EventHandler
    public void onJoin(ClientJoinEvent event) {
        event.setJoinMessage(null);
        event.getPlayer().setGameMode(GameMode.ADVENTURE);
        event.getPlayer().setTotalExperience(0);
        event.getPlayer().setExperienceLevelAndProgress(0);

        if (location.getWorld() == null) {
            location.setWorld(Bukkit.getWorld("world"));
        }
        event.getPlayer().teleport(location);
    }

    @EventHandler
    public void onQuit(ClientQuitEvent event) {
        event.setQuitMessage(null);
    }

}
