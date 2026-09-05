package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.world.resource.archetype.PersonalOreArchetype;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.zone.PlayerEnterZoneEvent;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Moves a player's per-player mine state in and out of memory around them.
 * <p>
 * {@link PersonalOreArchetype} owns the state; this owns the two moments it travels. It is <b>read on entering</b> a
 * node rather than on joining the server, because a player who has never been near the mine has nothing to read and
 * most never will, and it is <b>dropped on quit</b>, since it is only a cache of rows that stand without it. Showing
 * that state is not this listener's job: {@code PersonalOreView} rewrites the outgoing packets, so a chunk coming back
 * into view already arrives depleted.
 */
@Singleton
@BPvPListener
public class PersonalOreSessionListener implements Listener {

    /**
     * How long a depleted point is kept before housekeeping drops it. Only ever reached by players who stopped playing
     * mid-respawn: anyone who comes back has their expired points cleared and handed straight back on the way in, so
     * this is about the table not growing without bound rather than about the timer itself.
     */
    private static final long RETENTION_MS = TimeUnit.DAYS.toMillis(1);

    private final Clans clans;
    private final ResourceNodeManager nodes;
    private final ZoneManager zoneManager;
    private final PersonalOreArchetype archetype;
    private final PersonalMineRepository repository;

    @Inject
    public PersonalOreSessionListener(@NotNull Clans clans, @NotNull ResourceNodeManager nodes,
                                      @NotNull ZoneManager zoneManager, @NotNull PersonalOreArchetype archetype,
                                      @NotNull PersonalMineRepository repository) {
        this.clans = clans;
        this.nodes = nodes;
        this.zoneManager = zoneManager;
        this.archetype = archetype;
        this.repository = repository;
    }

    /**
     * Loads on any zone change rather than on the node's own zone being named, because overlapping zones report only
     * the highest-priority one as entered - a mine drawn inside a spawn safe area is two zones, and which of them the
     * event names is not this listener's business. Reconciled on the next tick, since the player is still standing at
     * the location they came from while the event fires.
     */
    @EventHandler
    public void onEnterZone(PlayerEnterZoneEvent event) {
        final Player player = event.getPlayer();
        UtilServer.runTask(clans, () -> {
            if (!player.isOnline()) {
                return;
            }
            for (Zone zone : zoneManager.getZonesAt(player.getLocation())) {
                final ResourceNodeProp node = nodes.byZone(zone.getKey());
                // Reference equality against the singleton, so this stays quiet for every other kind of node.
                // loadFor is idempotent, so being called again for a node already loaded costs nothing.
                if (node != null && node.getArchetype() == archetype) {
                    archetype.loadFor(node, player);
                }
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        archetype.evict(event.getPlayer().getUniqueId());
    }

    @UpdateEvent(delay = 60 * 60 * 1000)
    public void pruneAbandonedPoints() {
        repository.pruneOlderThan(System.currentTimeMillis() - RETENTION_MS);
    }
}
