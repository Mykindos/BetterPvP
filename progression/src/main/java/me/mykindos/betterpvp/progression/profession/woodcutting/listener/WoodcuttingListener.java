package me.mykindos.betterpvp.progression.profession.woodcutting.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.function.DoubleUnaryOperator;


/**
 * This class's purpose is to listen for whenever a block is broken
 * and notify the WoodcuttingHandler appropriately.
 */
@BPvPListener
@CustomLog
@Singleton
public class WoodcuttingListener implements Listener {
    private final WoodcuttingHandler woodcuttingHandler;

    @Inject
    public WoodcuttingListener(WoodcuttingHandler woodcuttingHandler) {
        this.woodcuttingHandler = woodcuttingHandler;
    }

    /**
     * Whenever a player breaks a block, this event will trigger and
     * 'attemptToChopLog' will handle things like: "was it a wood log type?" or
     * "how much xp will the player get (if any)"
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Material blockType = event.getBlock().getType();

        if (!woodcuttingHandler.getExperiencePerWood().containsKey(blockType)) return;

        Block choppedLogBlock = event.getBlock();
        PlayerChopLogEvent chopLogEvent = UtilServer.callEvent(new PlayerChopLogEvent(event.getPlayer(), blockType,
                choppedLogBlock));

        // if the player doesn't have tree feller, then add the log that was chopped manually here
        int amountChopped = chopLogEvent.getAmountChopped() > 0 ? chopLogEvent.getAmountChopped() : 1;

        DoubleUnaryOperator experienceModifier = (xp) -> xp * chopLogEvent.getExperienceBonusModifier();
        woodcuttingHandler.attemptToChopLog(event.getPlayer(), chopLogEvent.getLogType(), event.getBlock(), experienceModifier,
                amountChopped);
    }
}
