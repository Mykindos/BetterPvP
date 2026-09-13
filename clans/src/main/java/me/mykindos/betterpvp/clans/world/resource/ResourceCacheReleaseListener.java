package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.site.SiteWorldReleasedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Drops the resource-node records kept for a world that is being deleted, so a destroyed site's mid-respawn ore and
 * in-flight tree frames never outlive its folder.
 */
@CustomLog
@BPvPListener
@Singleton
public class ResourceCacheReleaseListener implements Listener {

    private final BlockBatchStore blockBatchStore;
    private final BlockReplacementStore blockReplacementStore;

    @Inject
    public ResourceCacheReleaseListener(@NotNull BlockBatchStore blockBatchStore,
                                        @NotNull BlockReplacementStore blockReplacementStore) {
        this.blockBatchStore = blockBatchStore;
        this.blockReplacementStore = blockReplacementStore;
    }

    @EventHandler
    public void onSiteWorldReleased(@NotNull SiteWorldReleasedEvent event) {
        final int batches = blockBatchStore.purgeWorld(event.getWorld());
        final int blocks = blockReplacementStore.purgeWorld(event.getWorld());
        if (batches > 0 || blocks > 0) {
            log.info("Purged resource-node cache for world '{}': {} block batch(es), {} block record(s)",
                    event.getWorld(), batches, blocks).submit();
        }
    }
}
