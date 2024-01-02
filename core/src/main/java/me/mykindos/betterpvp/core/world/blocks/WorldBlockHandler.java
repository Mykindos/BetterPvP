package me.mykindos.betterpvp.core.world.blocks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class WorldBlockHandler {

    @Getter
    private final Map<Block, RestoreBlock> restoreBlocks = new HashMap<>();
    @Getter
    private final Map<Runnable, Long> scheduledBlocks = new HashMap<>();
    @Inject
    private Core core;

    public void addRestoreBlock(Block block, Material newMaterial, long expiry) {
        addRestoreBlock(block, newMaterial, expiry, true);
    }

    /**
     * Adds a block to be restored
     * @param block Block to restore
     * @param newMaterial Material to restore to
     * @param expiry Time in milliseconds to restore
     * @param force Whether to override an existing restore block's expiry or choose the higher value
     */
    public void addRestoreBlock(Block block, Material newMaterial, long expiry, boolean force) {
        Optional<RestoreBlock> restoreBlockOptional = getRestoreBlock(block);
        if (restoreBlockOptional.isPresent()) {
            final long newExpiry = System.currentTimeMillis() + expiry;
            RestoreBlock restoreBlock = restoreBlockOptional.get();
            restoreBlock.setExpire(force ? newExpiry : Math.max(restoreBlock.getExpire(), newExpiry));
        } else {
            restoreBlocks.put(block, new RestoreBlock(block, newMaterial, expiry));
        }
    }

    public void scheduleRestoreBlock(Block block, Material newMaterial, long delay, long expiry) {
        scheduleRestoreBlock(block, newMaterial, delay, expiry, true);
    }

    /**
     * Adds a block to be restored after a delay
     * @param block Block to restore
     * @param newMaterial Material to restore to
     * @param delay Delay in milliseconds
     * @param expiry Time in milliseconds to restore
     * @param force Whether to override an existing restore block's expiry or choose the higher value
     */
    public void scheduleRestoreBlock(Block block, Material newMaterial, long delay, long expiry, boolean force) {
        this.scheduledBlocks.put(() -> addRestoreBlock(block, newMaterial, expiry, force), System.currentTimeMillis() + delay);
    }

    public boolean isRestoreBlock(Block block) {
        return restoreBlocks.containsKey(block);
    }

    public Optional<RestoreBlock> getRestoreBlock(Block block) {
        return Optional.ofNullable(restoreBlocks.get(block));
    }

    public void outlineChunk(Chunk chunk) {
        final BlockData data = Material.SEA_LANTERN.createBlockData();
        final Map<Location, BlockData> outline = new HashMap<>();
        final Map<Location, BlockData> original = new HashMap<>();
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                if (z == 0 || z == 15 || x == 0 || x == 15) {
                    Location blockLoc = chunk.getBlock(x, 0, z).getLocation();
                    Block down = chunk.getWorld().getHighestBlockAt(blockLoc, HeightMap.MOTION_BLOCKING_NO_LEAVES);
                    if (!(down.getState() instanceof Container)) {
                        original.put(down.getLocation(), down.getBlockData());
                        outline.put(down.getLocation(), data);
                    }
                }
            }
        }

        Bukkit.getOnlinePlayers().forEach(player -> player.sendMultiBlockChange(outline));
        UtilServer.runTaskLater(core, () -> Bukkit.getOnlinePlayers().forEach(player -> player.sendMultiBlockChange(original)), 60 * 20L);
    }
}
