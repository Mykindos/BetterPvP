package me.mykindos.betterpvp.core.block.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

@BPvPListener
@Singleton
@CustomLog
public class SmartBlockChunkListener implements Listener {
    
    private final SmartBlockDataManager dataManager;
    
    @Inject
    private SmartBlockChunkListener(SmartBlockDataManager dataManager) {
        this.dataManager = dataManager;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        dataManager.loadChunk(event.getChunk());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        dataManager.unloadChunk(event.getChunk());
    }
} 