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
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import javax.annotation.Nullable;
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


    /**
     * Global Map that maps a player's <code>UUID</code> to a <code>Set</code> of all saplings they have
     * planted
     */
    private final Map<UUID, Set<Block>> plantedSaplings = new HashMap<>();


    /**
     * Global Queue that holds all the saplings that <b>Forest Flourisher</b> has decided to speed up growth
     * for
     */
    private final Queue<Block> blocksToBeBoneMealed = new LinkedList<>();


    /**
     * The time, in milliseconds, between each update event where all player's saplings will try to grow
     * through <b>Forest Flourisher</b>
     */
    private final long cycleDuration = 60000L;


    /**
     * A number between 0 and 1 which represents the percentage that the growth factor is increased by per
     * level; this is a small decimal number
     */
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

    /**
     * @param level the player's skill level for <b>Forest Flourisher</b>
     * @return the chance for the player's saplings to grow faster; this number will be between 0 and 1
     */
    public double growFactor(int level) {
        return growFactorIncreasePerLvl * level;
    }

    @Override
    public Material getIcon() {
        return Material.BONE_MEAL;
    }

    /**
     * @param block any block in Minecraft
     * @return the corresponding <code>TreeType</code> for the given sapling <code>block</code>,
     * or, this method will return <code>null</code> if <code>block</code>'s type does not
     * correspond to a <code>TreeType</code>
     */
    public @Nullable TreeType getTreeType(Block block) {
        return switch (block.getType()) {
            case BIRCH_SAPLING -> TreeType.BIRCH;
            case DARK_OAK_SAPLING -> TreeType.DARK_OAK;
            case ACACIA_SAPLING -> TreeType.ACACIA;
            case OAK_SAPLING -> TreeType.TREE;
            case JUNGLE_SAPLING -> TreeType.SMALL_JUNGLE;
            case SPRUCE_SAPLING -> TreeType.REDWOOD;
            case MANGROVE_PROPAGULE -> TreeType.MANGROVE;
            default -> null;
        };
    }


    /**
     * <figure>
     *     <figcaption>This Listener will check that player...</figcaption>
     *     <ul>
     *         <li>Placed a sapling block</li>
     *         <li>Has the <b>Forest Flourisher</b> skill</li>
     *     </ul>
     * </figure>
     * Then, this method will add the player's UUID (key) & the block that was placed (value) to a global Map
     * @param event a BlockPlaceEvent that triggers when the player places a block
     */
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
    @UpdateEvent(delay = cycleDuration)
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
