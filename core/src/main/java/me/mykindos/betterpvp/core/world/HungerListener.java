package me.mykindos.betterpvp.core.world;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

@BPvPListener
public class HungerListener implements Listener {

    @Inject
    @Config(path = "hunger.enabled", defaultValue = "false")
    private boolean hungerEnabled;

    private final Core core;

    @Inject
    public HungerListener(Core core) {
        this.core = core;
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if(!hungerEnabled) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if(!hungerEnabled) {
            event.getPlayer().setFoodLevel(20);
            event.getPlayer().setSaturation(0);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!hungerEnabled) {
            UtilServer.runTaskLater(core, () -> {
                event.getPlayer().setSaturation(0);
                event.getPlayer().setFoodLevel(20);
            }, 1L);
        }
    }

}
