package me.mykindos.betterpvp.progression.profession.skill.woodcutting.attributes.autoreplant;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

@BPvPListener
@Singleton
public class AutoReplantAttributeListener implements Listener {

    private static final Set<Material> COMPATIBLE_SOIL = Set.of(
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.COARSE_DIRT,
            Material.PODZOL,
            Material.ROOTED_DIRT,
            Material.MOSS_BLOCK
    );

    private final WoodcuttingAutoReplantAttribute autoReplantAttribute;
    private final BlockTagManager blockTagManager;

    @Inject
    public AutoReplantAttributeListener(WoodcuttingAutoReplantAttribute autoReplantAttribute, BlockTagManager blockTagManager) {
        this.autoReplantAttribute = autoReplantAttribute;
        this.blockTagManager = blockTagManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChopLog(PlayerChopLogEvent event) {
        if (blockTagManager.isPlayerPlaced(event.getChoppedLogBlock())) return;
        if (!roll(autoReplantAttribute.getChance(event.getPlayer()))) return;

        Block logBlock = event.getChoppedLogBlock();
        Material sapling = getSapling(event.getLogType());
        if (sapling == null) return;
        if (!COMPATIBLE_SOIL.contains(logBlock.getRelative(0, -1, 0).getType())) return;

        UtilServer.runTaskLater(JavaPlugin.getPlugin(Progression.class), () -> {
            if (!logBlock.isEmpty()) return;
            if (!UtilInventory.remove(event.getPlayer(), sapling, 1)) return;

            logBlock.setType(sapling);
            logBlock.getWorld().playSound(logBlock.getLocation(), Sound.BLOCK_GRASS_PLACE, 1.0F, 1.0F);
        }, 1L);
    }

    private Material getSapling(Material logType) {
        return switch (logType) {
            case OAK_LOG -> Material.OAK_SAPLING;
            case BIRCH_LOG -> Material.BIRCH_SAPLING;
            case JUNGLE_LOG -> Material.JUNGLE_SAPLING;
            case ACACIA_LOG -> Material.ACACIA_SAPLING;
            case DARK_OAK_LOG -> Material.DARK_OAK_SAPLING;
            case SPRUCE_LOG -> Material.SPRUCE_SAPLING;
            case CHERRY_LOG -> Material.CHERRY_SAPLING;
            case PALE_OAK_LOG -> Material.PALE_OAK_SAPLING;
            default -> null;
        };
    }

    private boolean roll(double chance) {
        if (chance <= 0) return false;
        if (chance > 1) return Math.random() * 100.0 < chance;
        return Math.random() < chance;
    }
}
