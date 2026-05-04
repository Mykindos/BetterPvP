package me.mykindos.betterpvp.progression.profession.skill.mining.attributes.oreinfusion;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blockbreak.event.ScriptedBlockPlaceEvent;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@BPvPListener
@Singleton
public class OreInfusionAttributeListener implements Listener {

    // Weighted ore pool: coal and iron appear multiple times to weight them more heavily
    private static final Material[] ORE_POOL = {
            Material.COAL_ORE,
            Material.IRON_ORE,
            Material.COPPER_ORE,
            Material.GOLD_ORE,
            Material.GILDED_BLACKSTONE,
            Material.REDSTONE_ORE,
            Material.LAPIS_ORE,
            Material.EMERALD_ORE,
            Material.DIAMOND_ORE
    };

    private static final int MAX_VEIN_SIZE = 32;

    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN
    };

    private record VeinNode(Block block, int depth) {}

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
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() >= chance) return;

        final int radius = (int) Math.ceil(attribute.getRadius());
        final double oreChance = attribute.getOreChance();
        final Set<Block> visited = new HashSet<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    Block seed = minedBlock.getRelative(x, y, z);
                    if (visited.contains(seed)) continue;
                    if (!isReplaceable(seed)) continue;
                    if (random.nextDouble() >= oreChance) continue;

                    Material oreType = ORE_POOL[random.nextInt(ORE_POOL.length)];
                    spreadVein(event.getPlayer(), seed, minedBlock, oreType, radius, oreChance, visited, random);
                }
            }
        }
    }

    private void spreadVein(Player player, Block seed, Block origin, Material oreType,
                            int radius, double oreChance, Set<Block> visited, ThreadLocalRandom random) {
        Deque<VeinNode> queue = new ArrayDeque<>();
        visited.add(seed);
        queue.add(new VeinNode(seed, 1));

        int converted = 0;
        while (!queue.isEmpty() && converted < MAX_VEIN_SIZE) {
            VeinNode node = queue.poll();
            Block block = node.block();
            int depth = node.depth();

            if (!convertBlock(player, block, oreType)) continue;
            converted++;

            int nextDepth = depth + 1;
            double spreadChance = computeSpreadChance(oreChance, nextDepth);
            if (spreadChance <= 0.0) continue;

            for (BlockFace face : FACES) {
                Block neighbor = block.getRelative(face);
                if (!visited.add(neighbor)) continue;
                if (Math.abs(neighbor.getX() - origin.getX()) > radius) continue;
                if (Math.abs(neighbor.getY() - origin.getY()) > radius) continue;
                if (Math.abs(neighbor.getZ() - origin.getZ()) > radius) continue;
                if (!isReplaceable(neighbor)) continue;
                if (random.nextDouble() >= spreadChance) continue;

                queue.add(new VeinNode(neighbor, nextDepth));
            }
        }
    }

    /**
     * Per-block spread chance at a given BFS depth. depth == 1 is the seed (always placed),
     * each subsequent depth halves the chance: depth 2 = oreChance, depth 3 = oreChance/2, etc.
     */
    private double computeSpreadChance(double oreChance, int depth) {
        if (depth <= 1) return 1.0;
        return 0.5 / (1 << (depth - 1));
    }

    private boolean isReplaceable(Block block) {
        if (!UtilBlock.isStoneBased(block)) return false;
        if (UtilBlock.isOre(block.getType())) return false;
        if (blockTagManager.isPlayerPlaced(block)) return false;
        return true;
    }

    private boolean convertBlock(Player player, Block block, Material oreType) {
        if (!isReplaceable(block)) return false;

        final BlockData previousData = block.getBlockData();
        final BlockData replacementData = oreType.createBlockData();
        final ScriptedBlockPlaceEvent placeEvent = new ScriptedBlockPlaceEvent(
                player,
                block,
                previousData,
                replacementData,
                "progression:ore_infusion");
        placeEvent.callEvent();
        if (placeEvent.isCancelled()) return false;

        block.setBlockData(placeEvent.getReplacementData());
        UtilBlock.playBlockEffect(block, placeEvent.getReplacementData());
        return true;
    }
}
