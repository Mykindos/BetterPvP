package me.mykindos.betterpvp.progression.profession.skill.woodcutting.forestflourisher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionAttributeNode;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
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
@NodeId("forest_flourisher")
public class ForestFlourisher implements IProfessionAttribute {

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


    private final ProfessionProfileManager profileManager;


    @Inject
    public ForestFlourisher(ProfessionProfileManager profileManager) {
        this.profileManager = profileManager;
    }


    @Override
    public String getName() {
        return "Forest Flourisher";
    }

    @Override
    public String getDescription() {
        return "sapling growth acceleration chance";
    }

    @Override
    public String getOperation() {
        return "%";
    }

    @Override
    public double getDisplayValue(double value) {
        return value * 100.0;
    }

    public double growFactor(Player player) {
        return IProfessionAttribute.computeValue(player, "Woodcutting", this, profileManager);
    }

    public double growFactor(UUID playerUUID) {
        Optional<ProfessionProfile> optionalProfile = profileManager.getObject(playerUUID.toString());

        return optionalProfile
                .map(profile -> profile.getProfessionDataMap().get("Woodcutting"))
                .map(data -> data.getBuild().getNodes().entrySet().stream()
                        .filter(e -> e.getKey() instanceof ProfessionAttributeNode && e.getValue() > 0)
                        .mapToDouble(e -> ((ProfessionAttributeNode) e.getKey()).getAttributes().entrySet().stream()
                                .filter(attribute -> attribute.getKey().getClass() == getClass())
                                .mapToDouble(attribute -> attribute.getValue().getBaseValue()
                                        + Math.max(e.getValue() - 1, 0) * attribute.getValue().getPerLevel())
                                .sum())
                        .sum())
                .orElse(0.0);
    }

    /**
     * This function's purpose is to return a boolean that tells you if the player has the attribute
     * <b>Forest Flourisher</b>
     */
    public boolean doesPlayerHaveAttribute(Player player) {
        return growFactor(player) > 0;
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
            case CHERRY_SAPLING -> TreeType.CHERRY;
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
     * @param event a BlockPlaceEvent that triggers when the player places a block
     */
    public void onPlayerPlantSapling(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        TreeType treeType = getTreeType(event.getBlock());
        if (treeType == null) return;

        Player player = event.getPlayer();
        if (!doesPlayerHaveAttribute(player)) return;

        addSaplingForPlayer(player, event.getBlock());
    }

    /**
     * This method will add the player's UUID (key) & the block that was placed (value) to a global Map
     */
    public void addSaplingForPlayer(Player player, Block block) {
        UUID playerUUID = player.getUniqueId();

        Set<Block> saplingList = plantedSaplings.getOrDefault(playerUUID, null);
        if (saplingList == null) {
            saplingList = new HashSet<>();
            plantedSaplings.put(playerUUID, saplingList);
        }

        saplingList.add(block);
    }

    /**
     * This event's purpose is to determine which blocks need to be bone-mealed and offers them to the queue
     */
    public void increaseSaplingGrowthTime() {
        plantedSaplings.forEach((playerUUID, setOfBlocks) -> {
            setOfBlocks = setOfBlocks.stream()
                    .filter(block -> getTreeType(block) != null)
                    .collect(Collectors.toSet());

            double growFactor = growFactor(playerUUID);

            setOfBlocks.forEach(block -> {
                if (Math.random() < growFactor) {
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
    public void pollBlockToBoneMeal() {
        Block block = blocksToBeBoneMealed.poll();
        if (block != null && getTreeType(block) != null) {
            block.applyBoneMeal(BlockFace.UP);
        }
    }
}
