package me.mykindos.betterpvp.core.world.blocks;

import lombok.Data;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;


@Data
public class RestoreBlock {

    private final Block block;
    private final Material newMaterial;
    private final long expire;

    private BlockData blockData;
    private int blockLevel;

    public RestoreBlock(Block block, Material newMaterial, long expire) {
        this.block = block;
        this.newMaterial = newMaterial;
        this.expire = System.currentTimeMillis() + expire;
        this.blockData = block.getBlockData().clone();

        block.setType(newMaterial);
    }

    public void restore() {
        block.setBlockData(blockData);
    }

}
