package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.oreinfusion;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blockbreak.event.ScriptedBlockPlaceEvent;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.concurrent.ThreadLocalRandom;

@BPvPListener
@Singleton
public class OreInfusionAttributeListener implements Listener {

    // Weighted ore pool: coal and iron appear multiple times to weight them more heavily
    private static final Material[] ORE_POOL = {
            Material.COAL_ORE, Material.COAL_ORE, Material.COAL_ORE,
            Material.IRON_ORE, Material.IRON_ORE, Material.IRON_ORE,
            Material.COPPER_ORE, Material.COPPER_ORE,
            Material.GOLD_ORE,
            Material.REDSTONE_ORE,
            Material.LAPIS_ORE,
            Material.EMERALD_ORE,
            Material.DIAMOND_ORE
    };

    private final OreInfusionAttribute attribute;
    private final BlockTagManager blockTagManager;

    @Inject
    public OreInfusionAttributeListener(OreInfusionAttribute attribute, BlockTagManager blockTagManager) {
        this.attribute = attribute;
        this.blockTagManager = blockTagManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMinesOre(BlockBreakEvent event) {
        Block minedBlock = event.getBlock();
        if (!UtilBlock.isStoneBased(minedBlock)) return;
        if (UtilBlock.isOre(minedBlock.getType())) return;
        if (blockTagManager.isPlayerPlaced(minedBlock)) return;

        double chance = attribute.getChance(event.getPlayer());
        if (chance <= 0) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    Block neighbor = minedBlock.getRelative(x, y, z);
                    if (!UtilBlock.isStoneBased(neighbor)) continue;
                    if (UtilBlock.isOre(neighbor.getType())) continue;
                    if (blockTagManager.isPlayerPlaced(neighbor)) continue;
                    if (random.nextDouble() >= chance) continue;

                    final BlockData previousData = neighbor.getBlockData();
                    final BlockData replacementData = ORE_POOL[random.nextInt(ORE_POOL.length)].createBlockData();
                    final ScriptedBlockPlaceEvent placeEvent = new ScriptedBlockPlaceEvent(
                            event.getPlayer(),
                            neighbor,
                            previousData,
                            replacementData,
                            "progression:ore_infusion");
                    placeEvent.callEvent();
                    if (placeEvent.isCancelled()) continue;

                    neighbor.setBlockData(placeEvent.getReplacementData());
                    UtilBlock.playBlockEffect(neighbor, placeEvent.getReplacementData());
                }
            }
        }
    }
}
