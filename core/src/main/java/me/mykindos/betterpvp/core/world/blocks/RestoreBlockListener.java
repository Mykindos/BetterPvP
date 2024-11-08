package me.mykindos.betterpvp.core.world.blocks;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.inject.Inject;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
@BPvPListener
public class RestoreBlockListener implements Listener {

    private final MutableGraph<Block> collidingBlocks = GraphBuilder.undirected().build();
    private final WorldBlockHandler blockHandler;

    @Inject
    public RestoreBlockListener(WorldBlockHandler blockHandler) {
        this.blockHandler = blockHandler;
    }

    @EventHandler
    public void onBlock(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (blockHandler.isRestoreBlock(block)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPhysics(BlockFromToEvent event) {
        final Block block = event.getBlock();
        if (!UtilBlock.isWater(block) && !UtilBlock.isWater(event.getToBlock())) {
            return; // Not water
        }

        final boolean colliding = addCollidingRestoreBlock(block, block.getRelative(BlockFace.UP))
                || addCollidingRestoreBlock(block, block.getRelative(BlockFace.DOWN))
                || addCollidingRestoreBlock(block, block.getRelative(BlockFace.NORTH))
                || addCollidingRestoreBlock(block, block.getRelative(BlockFace.SOUTH))
                || addCollidingRestoreBlock(block, block.getRelative(BlockFace.EAST))
                || addCollidingRestoreBlock(block, block.getRelative(BlockFace.WEST));

        if (colliding) {
            event.setCancelled(true);
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRestoreBlockExtend(final BlockPistonExtendEvent event) {
        for (final Block block : event.getBlocks()) {
            if(blockHandler.isRestoreBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRestoreBlockRetract(final BlockPistonRetractEvent event) {
        for (final Block block : event.getBlocks()) {
            if(blockHandler.isRestoreBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onFallingBlockSpawn(EntityChangeBlockEvent event) {
        if (blockHandler.isRestoreBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @UpdateEvent
    public void processBlocks() {
        Iterator<RestoreBlock> restoreBlockListIterator = blockHandler.getRestoreBlocks().values().iterator();
        while (restoreBlockListIterator.hasNext()) {
            RestoreBlock next = restoreBlockListIterator.next();
            if (next.isRestored()) {
                restoreBlockListIterator.remove();
                continue;
            }

            if (System.currentTimeMillis() >= next.getExpire()) {
                restoreBlockListIterator.remove();
                next.restore();

                // If the block is colliding with another block, and it's not a restore block, attempt to update it
                if (this.collidingBlocks.nodes().contains(next.getBlock())) {
                    this.collidingBlocks.adjacentNodes(next.getBlock()).forEach(this::attemptUpdate);
                }
            }
        }

        // We do this to avoid scheduling tons of future tasks at once
        // Instead, we batch them
        final Iterator<Map.Entry<Runnable, Long>> scheduledIterator = blockHandler.getScheduledBlocks().entrySet().iterator();
        while (scheduledIterator.hasNext()) {
            final Map.Entry<Runnable, Long> next = scheduledIterator.next();
            if (System.currentTimeMillis() >= next.getValue()) {
                next.getKey().run();
                scheduledIterator.remove();
            }
        }
    }

    @EventHandler
    public void onStateChange(BlockFadeEvent event) {
        Block block = event.getBlock();
        if (blockHandler.isRestoreBlock(block)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSuffocationDamage(CustomDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
            Optional<RestoreBlock> restoreBlockOptional = blockHandler.getRestoreBlock(event.getDamagee().getEyeLocation().getBlock());
            if (restoreBlockOptional.isPresent()) {
                LivingEntity newDamager = restoreBlockOptional.get().getSummoner();
                if (newDamager != null && event.getDamager() == null) {
                    event.setDamager(newDamager);
                }
            }
        }
    }


    // Attempt to update a block if it's not colliding with any restore blocks
    private void attemptUpdate(Block block) {
        if (this.blockHandler.isRestoreBlock(block)) {
            return; // Return if the block is a restore block
        }

        if (this.collidingBlocks.nodes().contains(block) && !this.collidingBlocks.adjacentNodes(block).isEmpty()) {
            return; // Return if the block is colliding with anything
        }

        block.getState().update(false, true);
        this.collidingBlocks.removeNode(block);
    }

    // Attempt to add a colliding restore block to the graph
    // Only if the colliding block is a restore block
    private boolean addCollidingRestoreBlock(Block block, Block colliding) {
        if (this.blockHandler.isRestoreBlock(colliding)) {
            this.collidingBlocks.putEdge(block, colliding);
            return true;
        }
        return false;
    }
}
