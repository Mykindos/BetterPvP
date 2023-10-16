package me.mykindos.betterpvp.core.world;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

@BPvPListener
public class HungerListener implements Listener {

    @Inject
    @Config(path = "hunger.enabled", defaultValue = "false")
    private boolean hungerEnabled;

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if(!hungerEnabled) {
            if(event.getEntity().getFoodLevel() != 20) {
                event.getEntity().setFoodLevel(20);
                event.getEntity().setSaturation(0);
            }
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

}
