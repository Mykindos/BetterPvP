package me.mykindos.betterpvp.progression.tree.mining.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.tree.mining.Mining;
import me.mykindos.betterpvp.progression.tree.mining.MiningService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

@BPvPListener
@Slf4j
@Singleton
public class MiningStatsListener implements Listener {

    @Inject
    private MiningService service;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        service.attemptMineOre(event.getPlayer(), event.getBlock());
    }

}
