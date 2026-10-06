package me.mykindos.betterpvp.core.block.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
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
    private final Core plugin;

    @Inject
    private SmartBlockChunkListener(SmartBlockDataManager dataManager, Core plugin) {
        this.dataManager = dataManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        // Entities load a tick after the chunk, and furniture is found through its display entity
        UtilServer.runTaskLater(plugin, () -> dataManager.loadChunk(event.getChunk()), 1L);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        dataManager.unloadChunk(event.getChunk());
    }
} 