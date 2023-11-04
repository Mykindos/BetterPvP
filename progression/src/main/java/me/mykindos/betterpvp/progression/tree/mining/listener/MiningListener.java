package me.mykindos.betterpvp.progression.tree.mining.listener;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.tree.mining.event.ProgressionMiningEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.function.LongUnaryOperator;

@BPvPListener
@Slf4j
@Singleton
public class MiningListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak (BlockBreakEvent event) {
        log.info("called");
        UtilServer.callEvent(new ProgressionMiningEvent(event.getPlayer(), event.getBlock(), LongUnaryOperator.identity()));
    }

    @EventHandler
    public void onMine(ProgressionMiningEvent event) {
        log.info("handler");
    }

}