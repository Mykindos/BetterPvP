package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class TreeFeller extends WoodcuttingProgressionSkill implements Listener {

    WeakHashMap<UUID, HashSet<Block>> playerProcessedBlocksMap = new WeakHashMap<>();
    private final ProfessionProfileManager professionProfileManager;
    final int MAX_POSSIBLE_CHOPPED_LOGS = 15;

    @Inject
    public TreeFeller(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
    }

    @Override
    public String getName() {
        return "Tree Feller";
    }

    // add cooldown calculation in here!
    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Cut down an entire tree by chopping a single log",
                "Cooldown tbd"
        };
    }

    @Override
    public Material getIcon() {
        return Material.GOLDEN_AXE;
    }


    @EventHandler
    public void onPlayerChopsLog(PlayerChopLogEvent event) {

        Player player = event.getPlayer();
        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            var profession = profile.getProfessionDataMap().get("Woodcutting");
            if (profession != null) {
                int skillLevel = profession.getBuild().getSkillLevel(this);
                if (skillLevel <= 0) return;

                HashSet<Block> processedBlocks = new HashSet<>();

                // I don't think you'll have to clear the HashMap since this will just override the player's
                // processedBlocks everytime that they use this perk
                playerProcessedBlocksMap.put(player.getUniqueId(), processedBlocks);
                processBlock(player, event.getLogType(), event.getChoppedLogBlock());
            }
        });
    }

    /**
     * Recursive function that handles the tree feller algorithm
     */
    public void processBlock(Player player, Material initialChoppedLogType, Block currentBlock) {
        HashSet<Block> processedBlocks = playerProcessedBlocksMap.get(player.getUniqueId());

        if (processedBlocks.size() >= MAX_POSSIBLE_CHOPPED_LOGS || !currentBlock.getType().equals(initialChoppedLogType)) {
            return;
        }

        if (processedBlocks.contains(currentBlock)) return;

        World world = player.getWorld();

        Block nextBlock = currentBlock;
        while (processedBlocks.size() < MAX_POSSIBLE_CHOPPED_LOGS
                && nextBlock.getType().equals(initialChoppedLogType)
                && !processedBlocks.contains(nextBlock)) {

            nextBlock.breakNaturally(true);
            processedBlocks.add(nextBlock);

            int x = nextBlock.getX();
            int y = nextBlock.getY();
            int z = nextBlock.getZ();

            Block[] adjacentBlocks = {
                    world.getBlockAt(x + 1, y, z),
                    world.getBlockAt(x, y, z + 1),
                    world.getBlockAt(x + 1, y, z + 1),
                    world.getBlockAt(x - 1, y, z),
                    world.getBlockAt(x, y, z - 1),
                    world.getBlockAt(x - 1, y, z - 1),
                    world.getBlockAt(x + 1, y, z - 1),
                    world.getBlockAt(x - 1, y, z + 1),
            };

            for (Block adjacentBlock : adjacentBlocks) {
                if (adjacentBlock.getType().equals(initialChoppedLogType)) {
                    processBlock(player, initialChoppedLogType, adjacentBlock);
                }
            }

            nextBlock = world.getBlockAt(nextBlock.getX(), nextBlock.getY() + 1, nextBlock.getZ());
        }
    }
}
