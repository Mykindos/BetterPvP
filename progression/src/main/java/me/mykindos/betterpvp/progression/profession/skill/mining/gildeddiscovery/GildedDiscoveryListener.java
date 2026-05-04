package me.mykindos.betterpvp.progression.profession.skill.mining.gildeddiscovery;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;

@BPvPListener
@Singleton
public class GildedDiscoveryListener implements Listener {

    @Inject
    private GildedDiscovery skill;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockDropItemEvent event) {
        if (skill.getSkillLevel(event.getPlayer()) <= 0) return;
        skill.onBlockBreak(event);
    }
}
