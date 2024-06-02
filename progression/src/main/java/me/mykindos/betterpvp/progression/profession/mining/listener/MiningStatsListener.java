package me.mykindos.betterpvp.progression.profession.mining.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

@BPvPListener
@CustomLog
@Singleton
public class MiningStatsListener implements Listener {

    private final MiningHandler miningHandler;

    @Inject
    public MiningStatsListener(MiningHandler miningHandler) {
        this.miningHandler = miningHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        miningHandler.attemptMineOre(event.getPlayer(), event.getBlock());
    }

}
