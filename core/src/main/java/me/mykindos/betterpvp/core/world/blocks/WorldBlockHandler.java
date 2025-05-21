package me.mykindos.betterpvp.core.world.blocks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class WorldBlockHandler {

    @Getter
    private final Map<Block, RestoreBlock> restoreBlocks = new HashMap<>();
    @Getter
    private final Map<Runnable, Long> scheduledBlocks = new HashMap<>();
    @Inject
    private Core core;

    public void addRestoreBlock(Block block, Material newMaterial, long expiry) {
        addRestoreBlock(null, block, newMaterial, expiry, true);
    }

    /**
     * Adds a block to be restored
     *
     * @param entity      The entity that summoned this block
     * @param block       Block to restore
     * @param newMaterial Material to restore to
     * @param expiry      Time in milliseconds to restore
     * @param force       Whether to override an existing restore block's expiry or choose the higher value
     */
    public void addRestoreBlock(@Nullable LivingEntity entity, Block block, Material newMaterial, long expiry, boolean force, @Nullable String label) {
        addRestoreBlock(entity, block, block.getBlockData().clone(), newMaterial, expiry, force, label);
    }

    public void addRestoreBlock(@Nullable LivingEntity entity, Block block, Material newMaterial, long expiry, boolean force) {
        addRestoreBlock(entity, block, newMaterial, expiry, force, null);
    }

    public void addRestoreBlock(@Nullable LivingEntity entity, Block block, BlockData blockData, Material newMaterial, long expiry, boolean force, @Nullable String label) {
        Optional<RestoreBlock> restoreBlockOptional = getRestoreBlock(block);
        if (restoreBlockOptional.isPresent()) {
            final long newExpiry = System.currentTimeMillis() + expiry;
            RestoreBlock restoreBlock = restoreBlockOptional.get();
            if (entity != null) {
                restoreBlock.setSummoner(entity);
            }
            restoreBlock.setExpire(force ? newExpiry : Math.max(restoreBlock.getExpire(), newExpiry));
            if (restoreBlock.getNewMaterial() == Material.AIR) {
                restoreBlock.setNewMaterial(newMaterial);
                block.setType(newMaterial);
            }
        } else {
            if (block.getType().equals(Material.WATER) && !newMaterial.equals(Material.WATER)) {
                Block aboveBlock = block.getLocation().clone().add(0, 1, 0).getBlock();
                if (!aboveBlock.getType().isSolid()) {
                    addRestoreBlock(entity, aboveBlock, Material.AIR, expiry, force, label);
                }
            }
            RestoreBlock newRestoreBlock = new RestoreBlock(block, blockData, newMaterial, expiry, entity, label);
            restoreBlocks.put(block, newRestoreBlock);
        }
    }

    public void scheduleRestoreBlock(Block block, Material newMaterial, long delay, long expiry) {
        scheduleRestoreBlock(null, block, newMaterial, delay, expiry, true);
    }

    /**
     * Adds a block to be restored after a delay
     *
     * @param entity      The entity that summoned this block
     * @param block       Block to restore
     * @param newMaterial Material to restore to
     * @param delay       Delay in milliseconds
     * @param expiry      Time in milliseconds to restore
     * @param force       Whether to override an existing restore block's expiry or choose the higher value
     */
    public void scheduleRestoreBlock(@Nullable LivingEntity entity, Block block, Material newMaterial, long delay, long expiry, boolean force) {
        this.scheduledBlocks.put(() -> addRestoreBlock(entity, block, newMaterial, expiry, force), System.currentTimeMillis() + delay);
    }

    /**
     * Checks whether the specified block is currently marked as a restore block.
     *
     * @param block The block to be checked.
     * @return true if the block is present in the restore blocks collection, false otherwise.
     */
    public boolean isRestoreBlock(Block block) {
        return restoreBlocks.containsKey(block);
    }

    /**
     * Checks if the given block is associated with a {@link RestoreBlock} and if the associated
     * label matches the specified name (case-insensitive).
     *
     * @param block The target block to check.
     * @param name The name to compare against the label of the associated {@link RestoreBlock}.
     * @return {@code true} if the block is associated with a {@link RestoreBlock} and the label
     * matches the specified name, otherwise {@code false}.
     */
    public boolean isRestoreBlock(Block block, String name) {
        RestoreBlock restoreBlock = restoreBlocks.get(block);
        if (restoreBlock != null) {
            if (restoreBlock.getLabel() != null) {
                return restoreBlock.getLabel().equalsIgnoreCase(name);
            }
        }
        return false;
    }

    /**
     * Retrieves the restore block associated with the specified block.
     *
     * @param block The block to check for an associated restore block.
     * @return An {@link Optional} containing the restore block if it exists, or an empty {@link Optional} if no restore block is associated.
     */
    public Optional<RestoreBlock> getRestoreBlock(Block block) {
        return Optional.ofNullable(restoreBlocks.get(block));
    }

    /**
     * Retrieves a list of {@code RestoreBlock} objects that match the specified summoner and label.
     *
     * @param summoner The entity that summoned the restore blocks being queried. Must not be null.
     * @param label    The label associated with the restore blocks being queried. Must not be null.
     * @return A list of {@code RestoreBlock} objects matching the given summoner and label.
     */
    public List<RestoreBlock> getRestoreBlocks(@NotNull LivingEntity summoner, @NotNull String label) {
        return restoreBlocks.values().stream().filter(restoreBlock -> restoreBlock.getSummoner() == summoner && Objects.equals(restoreBlock.getLabel(), label)).toList();
    }

    /**
     * Outlines the edges of a specified chunk with a predefined block type (Sea Lantern)
     * and reverts the change after a delay. This visual modification is applied using
     * multi-block changes and does not permanently alter the chunk's blocks.
     *
     * @param chunk The chunk to outline. The edges of the chunk (x = 0, x = 15, z = 0, z = 15)
     *              will be included, excluding locations with container blocks.
     */
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
