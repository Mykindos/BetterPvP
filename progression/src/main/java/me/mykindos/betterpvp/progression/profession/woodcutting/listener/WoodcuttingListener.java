package me.mykindos.betterpvp.progression.profession.woodcutting.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerStripLogEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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
    private final WorldBlockHandler worldBlockHandler;

    @Inject
    public WoodcuttingListener(WoodcuttingHandler woodcuttingHandler, WorldBlockHandler worldBlockHandler) {
        this.woodcuttingHandler = woodcuttingHandler;
        this.worldBlockHandler = worldBlockHandler;
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

        ItemStack toolUsed = event.getPlayer().getInventory().getItemInMainHand();
        Block choppedLogBlock = event.getBlock();
        PlayerChopLogEvent chopLogEvent = UtilServer.callEvent(
                new PlayerChopLogEvent(event.getPlayer(), blockType, choppedLogBlock, toolUsed));

        // if the player doesn't have tree feller, then add the log that was chopped manually here
        if (chopLogEvent.getAmountChopped() <= 0) {
            chopLogEvent.setAmountChopped(1);
        }

        DoubleUnaryOperator experienceModifier = (xp) -> xp * chopLogEvent.getExperienceBonusModifier();
        woodcuttingHandler.attemptToChopLog(chopLogEvent, experienceModifier);
    }

    /**
     * Whenever a play strips a log, this event will trigger and fire the appropriate custom event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void whenPlayerStripsALog(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!UtilBlock.isNonStrippedLog(block.getType())) return;

        if(worldBlockHandler.isRestoreBlock(block)) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        if (!UtilItem.isAxe(player.getInventory().getItemInMainHand())) return;

        UtilServer.callEvent(new PlayerStripLogEvent(player, block, event.useInteractedBlock(), event.useItemInHand()));
    }
}
