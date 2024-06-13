package me.mykindos.betterpvp.progression.profession.woodcutting.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

@BPvPListener
@CustomLog
@Singleton
public class WoodcuttingStatsListener implements Listener {
    private final WoodcuttingHandler woodcuttingHandler;

    @Inject
    public WoodcuttingStatsListener(WoodcuttingHandler woodcuttingHandler) {
        this.woodcuttingHandler = woodcuttingHandler;
    }

    /**
     * Whenever a player breaks a block, this event will trigger and
     * 'attemptToMineWood' will handle things like: "was it a wood log type?" or
     * "how much xp will the player get (if any)"
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        woodcuttingHandler.attemptToMineWood(event.getPlayer(), event.getBlock());
    }
}
