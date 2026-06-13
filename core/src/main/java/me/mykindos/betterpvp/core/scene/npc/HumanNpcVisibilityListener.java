package me.mykindos.betterpvp.core.scene.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scene.SceneObjectRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Collection;

/**
 * Maintains client-side visibility of packet-only {@link HumanNPC}s. Their NMS entities are
 * never added to the world, so no vanilla entity tracker (re-)sends them — this listener does
 * that job: it periodically shows the NPC to players entering range and hides it from players
 * leaving range, and invalidates the shown state when the client silently discards entities
 * (world change, respawn) so the next pass re-sends them.
 */
@BPvPListener
@Singleton
public class HumanNpcVisibilityListener implements Listener {

    private final SceneObjectRegistry registry;

    @Inject
    public HumanNpcVisibilityListener(SceneObjectRegistry registry) {
        this.registry = registry;
    }

    /**
     * The tracker pass. Show inside 48 blocks, hide beyond 56 — the gap prevents
     * show/hide flicker for players hovering at the boundary. This also covers joins:
     * a joining player is simply a viewer the NPC is not yet shown to.
     */
    @UpdateEvent(delay = 500)
    public void trackVisibility() {
        final Collection<HumanNPC> npcs = registry.getObjects(HumanNPC.class);
        if (npcs.isEmpty()) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            for (HumanNPC npc : npcs) {
                final Location npcLocation = npc.getHandle().getLocation();
                final boolean sameWorld = player.getWorld().equals(npcLocation.getWorld());
                if (npc.isShownTo(player)) {
                    if (!sameWorld || player.getLocation().distanceSquared(npcLocation) > 128 * 128) {
                        npc.hideTo(player);
                    }
                } else if (sameWorld && player.getLocation().distanceSquared(npcLocation) <= 90 * 90) {
                    npc.showTo(player);
                }
            }
        }
    }

    /**
     * The respawn screen reloads the client's dimension, discarding every tracked entity —
     * even when respawning into the same world. Invalidate so the tracker re-sends.
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        markAllHidden(event.getPlayer());
    }

    /** Switching worlds discards every client-side entity of the previous world. */
    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        markAllHidden(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        markAllHidden(event.getPlayer());
    }

    private void markAllHidden(Player player) {
        for (HumanNPC npc : registry.getObjects(HumanNPC.class)) {
            npc.markHidden(player);
        }
    }
}
