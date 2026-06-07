package me.mykindos.betterpvp.core.item.pagination;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A single, server-wide clock that advances lore-page rotation for menu tiles.
 * <p>
 * High-cardinality menus (e.g. the item viewer with dozens of tiles per page) must not each spawn
 * their own timer. Instead, every rotating tile registers here while it is shown and unregisters
 * when removed; one {@link UpdateEvent} ticks them all together. Combined with per-tile page
 * caching, a tick only swaps a pre-rendered stack, so rotation never appears in a profiler.
 */
@BPvPListener
@Singleton
public class LoreRotationClock implements Listener {

    /**
     * A menu tile whose lore page rotates on the shared clock.
     */
    public interface Rotatable {
        /**
         * Advance to the next page and redraw. Called on the main thread once per tick.
         */
        void rotateTick();
    }

    private final Set<Rotatable> rotatables = ConcurrentHashMap.newKeySet();

    public void register(Rotatable rotatable) {
        rotatables.add(rotatable);
    }

    public void unregister(Rotatable rotatable) {
        rotatables.remove(rotatable);
    }

    @UpdateEvent(delay = 1000)
    public void tick() {
        if (rotatables.isEmpty()) {
            return;
        }
        for (Rotatable rotatable : rotatables) {
            rotatable.rotateTick();
        }
    }

}
