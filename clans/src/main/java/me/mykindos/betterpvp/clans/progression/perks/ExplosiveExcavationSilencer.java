package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.events.TerritoryInteractEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.mining.item.interaction.ExplosiveExcavationInteraction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Suppresses the "You cannot break X in Clan Y" chat output produced by
 * {@code ClansWorldListener#onBlockBreak} for blocks broken inside
 * {@link ExplosiveExcavationInteraction#detonate}. Without this, denying clans (foreign
 * territory or recruit-rank in own clan) would emit one message per block in the crater.
 *
 * <p>Only loads when the {@code Clans} plugin is present, so progression remains a soft
 * runtime dependency on clans even though it compile-references {@link TerritoryInteractEvent}.
 */
@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class ExplosiveExcavationSilencer implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTerritoryInteract(TerritoryInteractEvent event) {
        if (event.getInteractionType() != TerritoryInteractEvent.InteractionType.BREAK) {
            return;
        }
        if (ExplosiveExcavationInteraction.isSilentBreak(event.getBlock().getLocation())) {
            event.setInform(false);
        }
    }
}
