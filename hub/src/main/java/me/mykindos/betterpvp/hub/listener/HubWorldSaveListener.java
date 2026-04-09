package me.mykindos.betterpvp.hub.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.hub.model.HubWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldSaveEvent;

@BPvPListener
@Singleton
public class HubWorldSaveListener implements Listener {

    private final HubWorld hubWorld;

    @Inject
    private HubWorldSaveListener(HubWorld hubWorld) {
        this.hubWorld = hubWorld;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (!event.getWorld().getName().equals(hubWorld.getName())) {
            return;
        }

        event.setSaveChunk(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldSave(WorldSaveEvent event) {
        if (!event.getWorld().getName().equals(hubWorld.getName())) {
            return;
        }

        event.getWorld().setAutoSave(false);
    }
}
