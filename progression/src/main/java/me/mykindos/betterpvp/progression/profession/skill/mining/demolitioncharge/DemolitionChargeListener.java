package me.mykindos.betterpvp.progression.profession.skill.mining.demolitioncharge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

@BPvPListener
@Singleton
public class DemolitionChargeListener implements Listener {

    @Inject
    private DemolitionCharge skill;

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (skill.getSkillLevel(event.getPlayer()) <= 0) return;
        skill.onInteract(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // No skill level check — any marked block should still trigger even if skill was lost
        skill.onBlockBreak(event);
    }
}
