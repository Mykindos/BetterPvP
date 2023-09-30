package me.mykindos.betterpvp.core.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;

@BPvPListener
@Singleton
public class CoreWorldListener implements Listener {

    private final WorldHandler worldHandler;

    @Inject
    public CoreWorldListener(WorldHandler worldHandler) {
        this.worldHandler = worldHandler;
    }

    @EventHandler
    public void onLoadWorld(WorldLoadEvent event) {
        if(event.getWorld().getName().equals("world")) {
            worldHandler.loadSpawnLocations();
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(worldHandler.getSpawnLocation());
    }
}
