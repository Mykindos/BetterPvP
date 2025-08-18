package me.mykindos.betterpvp.game.framework.listener.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

@BPvPListener
@Singleton
public class WorldListener implements Listener {

    @Inject
    private WorldListener() {
    }

    @EventHandler
    public void onFallBlock(EntityChangeBlockEvent event) {
        if (event.getEntityType() != EntityType.FALLING_BLOCK) return;
        event.setCancelled(true);
    }
}
