package me.mykindos.betterpvp.core.block.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.Optional;

@BPvPListener
@Singleton
@CustomLog
public class SmartBlockRemovalListener implements Listener {

    private final Core core;
    private final SmartBlockFactory smartBlockFactory;
    private final SmartBlockDataManager smartBlockDataManager;

    @Inject
    private SmartBlockRemovalListener(Core core, SmartBlockFactory smartBlockFactory, SmartBlockDataManager smartBlockDataManager) {
        this.core = core;
        this.smartBlockFactory = smartBlockFactory;
        this.smartBlockDataManager = smartBlockDataManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        removeSmartBlock(event.getBlock(), BlockRemovalCause.NATURAL);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        removeSmartBlock(event.getBlock(), BlockRemovalCause.NATURAL);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().forEach(block -> removeSmartBlock(block, BlockRemovalCause.NATURAL));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        removeSmartBlock(event.getBlock(), BlockRemovalCause.NATURAL);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (smartBlockFactory.isSmartBlock(block)) {
                event.setCancelled(true);
                return; // Prevent pistons from pushing smart blocks
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky()) return;
        for (Block block : event.getBlocks()) {
            if (smartBlockFactory.isSmartBlock(block)) {
                event.setCancelled(true);
                return; // Prevent sticky pistons from retracting smart blocks
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent event) {
        removeSmartBlock(event.getBlock(), BlockRemovalCause.NATURAL);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPrime(TNTPrimeEvent event) {
        removeSmartBlock(event.getBlock(), BlockRemovalCause.NATURAL);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpongeAbsorb(SpongeAbsorbEvent event) {
        event.getBlocks().forEach(state -> removeSmartBlock(state.getBlock(), BlockRemovalCause.NATURAL));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        event.getBlocks().forEach(block -> {
            removeSmartBlock(block.getBlock(), BlockRemovalCause.NATURAL);
        });
    }

    private void removeSmartBlock(Block block, BlockRemovalCause cause) {
        Optional<SmartBlockInstance> instance = smartBlockFactory.from(block);
        if (instance.isEmpty()) return;

        // Run with delay to allow other plugins to read the block first
        UtilServer.runTaskLater(core, () -> {
            smartBlockDataManager.removeData(instance.get(), cause);
        }, 1L);
    }
}