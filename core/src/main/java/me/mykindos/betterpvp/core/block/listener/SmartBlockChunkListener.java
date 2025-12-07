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
        // we have to delay this by 1 tick because entities in this world have not loaded yet,
        // which won't allow our SmartBlockFactory to pick up on blocks marked by base entities (nexo)
        // and properly load them
        UtilServer.runTaskLater(plugin, () -> dataManager.loadChunk(event.getChunk()), 1L);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        dataManager.unloadChunk(event.getChunk());
    }
} 