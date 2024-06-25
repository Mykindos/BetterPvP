package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillDependency;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.TreeType;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
@BPvPListener
public class ForestFlourisher extends WoodcuttingProgressionSkill implements Listener {
    private final ProfessionProfileManager professionProfileManager;

    private final Map<UUID, Set<Block>> plantedSaplings = new HashMap<>();
    private final Queue<Block> blocksToBeBoneMealed = new LinkedList<>();

    private double growFactorIncreasePerLvl;


    @Inject
    public ForestFlourisher(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
    }

    @Override
    public String getName() {
        return "Forest Flourisher";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Saplings you plant grow <green>" + (growFactor(level)*100) + "%</green> faster"
        };
    }

    public double growFactor(int level) {
        return growFactorIncreasePerLvl * level;
    }

    @Override
    public Material getIcon() {
        return Material.BONE_MEAL;
    }

    public TreeType getTreeType(Block block) {
        return switch (block.getType()) {
            case BIRCH_SAPLING -> TreeType.BIRCH;
            case DARK_OAK_SAPLING -> TreeType.DARK_OAK;
            case ACACIA_SAPLING -> TreeType.ACACIA;
            case OAK_SAPLING -> TreeType.TREE;
            case JUNGLE_SAPLING -> TreeType.SMALL_JUNGLE;
            case SPRUCE_SAPLING -> TreeType.REDWOOD;
            default -> null;
        };
    }

    @EventHandler
    public void onPlayerPlantSapling(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        TreeType treeType = getTreeType(event.getBlock());
        if (treeType == null) return;

        Player player = event.getPlayer();
        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {

            int skillLevel = getPlayerSkillLevel(profile);
            if (skillLevel <= 0) return;

            UUID playerUUID = player.getUniqueId();

            Set<Block> saplingList = plantedSaplings.getOrDefault(playerUUID, null);
            if (saplingList == null) {
                saplingList = new HashSet<>();
                plantedSaplings.put(player.getUniqueId(), saplingList);
            }

            saplingList.add(event.getBlock());
        });
    }

    /**
     * This event's purpose is to determine which blocks need to be bone-mealed and offers them to the queue
     */
    @UpdateEvent(delay = 5000L)
    public void increaseSaplingGrowthTime() {
        plantedSaplings.forEach((playerUUID, setOfBlocks) -> {
            setOfBlocks = setOfBlocks.stream()
                    .filter(block -> getTreeType(block) != null)
                    .collect(Collectors.toSet());

            Optional<ProfessionProfile> optionalProfile = professionProfileManager.getObject(playerUUID.toString());
            int skillLevel = optionalProfile.map(this::getPlayerSkillLevel).orElse(0);

            setOfBlocks.forEach(block -> {
                if (Math.random() < growFactor(skillLevel)) {
                    blocksToBeBoneMealed.offer(block);
                }
            });

            plantedSaplings.put(playerUUID, setOfBlocks);
        });
    }

    /**
     * This event's purpose is to dequeue the sapling block in the front of the queue every tick and
     * apply bone meal to it
     */
    @UpdateEvent
    public void pollBlockToBoneMeal() {
        Block block = blocksToBeBoneMealed.poll();
        if (block != null && getTreeType(block) != null) {
            block.applyBoneMeal(BlockFace.UP);
        }
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        growFactorIncreasePerLvl = getConfig("growFactorIncreasePerLvl ", 0.003, Double.class);
    }

    @Override
    public ProgressionSkillDependency getDependencies() {
        final String[] dependencies = new String[]{"Tree Feller"};
        return new ProgressionSkillDependency(dependencies, 20);
    }
}
