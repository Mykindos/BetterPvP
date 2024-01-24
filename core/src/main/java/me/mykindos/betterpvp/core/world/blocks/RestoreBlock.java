package me.mykindos.betterpvp.core.world.blocks;

import lombok.Data;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;


@Data
public class RestoreBlock {

    private final Block block;
    private final Material newMaterial;
    private long expire;

    private BlockData blockData;
    private int blockLevel;

    @Nullable
    private Player summoner;

    public RestoreBlock(Block block, Material newMaterial, long expire, @Nullable Player summoner) {
        this.block = block;
        this.newMaterial = newMaterial;
        this.expire = System.currentTimeMillis() + expire;
        this.blockData = block.getBlockData().clone();
        this.summoner = summoner;

        block.setType(newMaterial);
    }

    public void restore() {
        block.setBlockData(blockData);
        // Update nearby blocks
        UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> block.getState().update(false, true), 1L);
    }

}
