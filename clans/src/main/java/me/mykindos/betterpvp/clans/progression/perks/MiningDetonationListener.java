package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.zone.ZoneInteraction;
import me.mykindos.betterpvp.core.world.zone.ZoneInteractEvent;
import me.mykindos.betterpvp.progression.profession.mining.util.MiningDetonation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Suppresses the "You cannot break X in Clan Y" denial message (emitted by the clans territory zone handler) for
 * blocks broken inside {@link MiningDetonation#detonate}. Without this, denying clans (foreign territory or recruit-rank
 * in own clan) would emit one message per block in the crater.
 *
 * <p>Runs at {@code LOWEST} so the {@code inform} flag is cleared before the clans handler decides; the denial message
 * itself is gated on the final {@code inform} state at {@code MONITOR}.
 */
@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class MiningDetonationListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onZoneInteract(ZoneInteractEvent event) {
        if (event.getInteraction() != ZoneInteraction.BREAK || event.getBlock() == null) {
            return;
        }
        if (MiningDetonation.isSilentBreak(event.getBlock().getLocation())) {
            event.setInform(false);
        }
    }
}
