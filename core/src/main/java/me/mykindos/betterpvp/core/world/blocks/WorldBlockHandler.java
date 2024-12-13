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
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                if (aboveBlock.getType().equals(Material.LILY_PAD)) {
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

    public boolean isRestoreBlock(Block block) {
        return restoreBlocks.containsKey(block);
    }

    public boolean isRestoreBlock(Block block, String name) {
        RestoreBlock restoreBlock = restoreBlocks.get(block);
        if (restoreBlock != null) {
            if (restoreBlock.getLabel() != null) {
                return restoreBlock.getLabel().equalsIgnoreCase(name);
            }
        }
        return false;
    }

    public Optional<RestoreBlock> getRestoreBlock(Block block) {
        return Optional.ofNullable(restoreBlocks.get(block));
    }

    public List<RestoreBlock> getRestoreBlocks(@NotNull LivingEntity summoner, @NotNull String label) {
        return restoreBlocks.values().stream().filter(restoreBlock -> restoreBlock.getSummoner() == summoner && Objects.equals(restoreBlock.getLabel(), label)).toList();
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
