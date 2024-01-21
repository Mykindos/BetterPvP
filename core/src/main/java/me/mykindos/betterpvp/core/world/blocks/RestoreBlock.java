package me.mykindos.betterpvp.core.world.blocks;

import lombok.Data;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;


@Data
public class RestoreBlock {

    private final Block block;
    private final Material newMaterial;
    private long expire;

    private BlockData blockData;
    private int blockLevel;

    public RestoreBlock(Block block, Material newMaterial, long expire) {
        this.block = block;
        this.newMaterial = newMaterial;
        this.expire = System.currentTimeMillis() + expire;
        this.blockData = block.getBlockData().clone();

        block.setType(newMaterial);
    }

    public void addSummoner(Player player) {
        PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(block);
        pdc.set(CoreNamespaceKeys.BLOCK_SUMMONER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
        UtilBlock.setPersistentDataContainer(block, pdc);
    }

    public void restore() {
        block.setBlockData(blockData);
        PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(block);
        pdc.remove(CoreNamespaceKeys.BLOCK_SUMMONER_KEY);
        UtilBlock.setPersistentDataContainer(block, pdc);
        // Update nearby blocks
        UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> block.getState().update(false, true), 1L);
    }

}
