package me.mykindos.betterpvp.core.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.configuration.GlobalConfiguration;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;

@BPvPListener
@Singleton
public class CoreWorldListener implements Listener {

    private final WorldHandler worldHandler;

    @Inject
    public CoreWorldListener(WorldHandler worldHandler) {
        this.worldHandler = worldHandler;
        ((CraftServer) Bukkit.getServer()).getServer().setFlightAllowed(true);
        GlobalConfiguration.get().collisions.enablePlayerCollisions = false;
    }

    @EventHandler
    public void onLoadWorld(WorldLoadEvent event) {
        if(event.getWorld().getName().equals("world")) {
            worldHandler.loadSpawnLocations();
        }

        var paperConfig = ((CraftWorld) event.getWorld()).getHandle().getLevel().paperConfig();
        paperConfig.misc.disableRelativeProjectileVelocity = true;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(worldHandler.getSpawnLocation());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if(!event.getPlayer().hasPlayedBefore()) {
            event.getPlayer().teleport(worldHandler.getSpawnLocation());
        }
    }
}
