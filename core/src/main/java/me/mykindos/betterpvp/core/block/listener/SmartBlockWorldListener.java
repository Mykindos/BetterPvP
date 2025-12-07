package me.mykindos.betterpvp.core.block.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;

@BPvPListener
@Singleton
@CustomLog
public class SmartBlockWorldListener implements Listener {

    private final SmartBlockDataManager dataManager;

    @Inject
    private SmartBlockWorldListener(SmartBlockDataManager dataManager) {
        this.dataManager = dataManager;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldSave(WorldSaveEvent event) {
        dataManager.saveWorld(event.getWorld());
    }
} 