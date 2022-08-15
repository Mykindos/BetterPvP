package me.mykindos.betterpvp.core.world.blocks;

import com.google.inject.Singleton;
import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class WorldBlockHandler {

    @Getter
    private final List<RestoreBlock> restoreBlocks;

    public WorldBlockHandler() {
        this.restoreBlocks = new ArrayList<>();
    }

    public void addRestoreBlock(Block block, Material newMaterial, long expiry) {
        Optional<RestoreBlock> restoreBlockOptional = getRestoreBlock(block);
        if(restoreBlockOptional.isPresent()) {
            RestoreBlock restoreBlock = restoreBlockOptional.get();
            restoreBlock.setExpire(System.currentTimeMillis() + expiry);
        }else{
            restoreBlocks.add(new RestoreBlock(block, newMaterial, expiry));
        }
    }

    public boolean isRestoreBlock(Block block) {
        return restoreBlocks.stream().anyMatch(restoreBlock -> restoreBlock.getBlock().getLocation().equals(block.getLocation()));
    }

    public Optional<RestoreBlock> getRestoreBlock(Block block) {
        return restoreBlocks.stream().filter(restoreBlock -> restoreBlock.getBlock().getLocation().equals(block.getLocation())).findFirst();
    }


    public void outlineChunk(Chunk chunk) {
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                if (z == 0 || z == 15 || x == 0 || x == 15) {
                    Location blockLoc = chunk.getBlock(x, 0, z).getLocation();
                    Block down = chunk.getWorld().getHighestBlockAt(blockLoc, HeightMap.MOTION_BLOCKING_NO_LEAVES);
                    if (!(down.getState() instanceof Container)) {
                        addRestoreBlock(down, Material.SEA_LANTERN, 60_000L);
                    }
                }
            }
        }

    }

}
