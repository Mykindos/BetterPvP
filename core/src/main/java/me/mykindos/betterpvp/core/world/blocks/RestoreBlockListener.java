package me.mykindos.betterpvp.core.world.blocks;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.inject.Inject;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;

import java.util.Iterator;
import java.util.Map;

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
        if (!UtilBlock.isWater(event.getBlock()) && !UtilBlock.isWater(event.getToBlock())) {
            return; // Not water
        }

        final Block block = event.getBlock();
        final boolean colliding = addCollidingRestoreBlock(event.getBlock(), block.getRelative(BlockFace.UP))
                || addCollidingRestoreBlock(event.getBlock(), block.getRelative(BlockFace.DOWN))
                || addCollidingRestoreBlock(event.getBlock(), block.getRelative(BlockFace.NORTH))
                || addCollidingRestoreBlock(event.getBlock(), block.getRelative(BlockFace.SOUTH))
                || addCollidingRestoreBlock(event.getBlock(), block.getRelative(BlockFace.EAST))
                || addCollidingRestoreBlock(event.getBlock(), block.getRelative(BlockFace.WEST));

        if (colliding) {
            event.setCancelled(true);
        }
    }

    @UpdateEvent
    public void processBlocks() {
        Iterator<RestoreBlock> restoreBlockListIterator = blockHandler.getRestoreBlocks().values().iterator();
        while (restoreBlockListIterator.hasNext()) {
            RestoreBlock next = restoreBlockListIterator.next();
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
