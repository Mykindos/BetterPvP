package me.mykindos.betterpvp.progression.profession.skill.woodcutting.forestflourisher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.progression.event.ProfessionPropertyUpdateEvent;
import me.mykindos.betterpvp.progression.profession.skill.IProfessionAttribute;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionAttributeNode;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
@NodeId("forest_flourisher")
public class ForestFlourisher implements IProfessionAttribute, Listener {

    public record SaplingRef(UUID worldId, int x, int y, int z, long plantedAt) {
    }

    /**
     * Global Map that maps a player's <code>UUID</code> to a <code>Set</code> of all saplings they have
     * planted
     */
    private final Map<UUID, Set<SaplingRef>> plantedSaplings = new HashMap<>();
    private final Set<SaplingRef> queuedSaplings = new HashSet<>();

    /**
     * Global Queue that holds all the saplings that <b>Forest Flourisher</b> has decided to speed up growth
     * for
     */
    private final Queue<SaplingRef> blocksToBeBoneMealed = new ArrayDeque<>();

    private final Map<UUID, Double> growFactorCache = new HashMap<>();

    private static final long SAPLING_EXPIRY_MS = TimeUnit.HOURS.toMillis(1);


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
        return growFactor(player.getUniqueId());
    }

    public double growFactor(UUID playerUUID) {
        return growFactorCache.computeIfAbsent(playerUUID, uuid -> {
            Optional<ProfessionProfile> optionalProfile = profileManager.getObject(uuid.toString());

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
        });
    }

    @EventHandler
    public void onProfileUpdate(ProfessionPropertyUpdateEvent event) {
        if (event.getProfession().equalsIgnoreCase("Woodcutting")) {
            growFactorCache.remove(event.getUuid());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        growFactorCache.remove(event.getPlayer().getUniqueId());
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

        Set<SaplingRef> saplingList = plantedSaplings.computeIfAbsent(playerUUID, k -> new HashSet<>());
        saplingList.add(new SaplingRef(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), System.currentTimeMillis()));
    }

    /**
     * This event's purpose is to determine which blocks need to be bone-mealed and offers them to the queue
     */
    public void increaseSaplingGrowthTime() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Set<SaplingRef>>> playerIterator = plantedSaplings.entrySet().iterator();

        while (playerIterator.hasNext()) {
            Map.Entry<UUID, Set<SaplingRef>> entry = playerIterator.next();
            UUID playerUUID = entry.getKey();
            Set<SaplingRef> saplings = entry.getValue();

            double growFactor = growFactor(playerUUID);
            Iterator<SaplingRef> saplingIterator = saplings.iterator();

            while (saplingIterator.hasNext()) {
                SaplingRef ref = saplingIterator.next();

                // 1. Check TTL
                if (now - ref.plantedAt() > SAPLING_EXPIRY_MS) {
                    saplingIterator.remove();
                    continue;
                }

                World world = Bukkit.getWorld(ref.worldId());
                if (world == null) {
                    saplingIterator.remove();
                    continue;
                }

                // Chunk-safe check
                if (!world.isChunkLoaded(ref.x() >> 4, ref.z() >> 4)) {
                    continue;
                }

                Block block = world.getBlockAt(ref.x(), ref.y(), ref.z());
                if (getTreeType(block) == null) {
                    saplingIterator.remove();
                    continue;
                }

                if (Math.random() < growFactor && queuedSaplings.add(ref)) {
                    blocksToBeBoneMealed.offer(ref);
                }
            }

            if (saplings.isEmpty()) {
                playerIterator.remove();
            }
        }
    }

    /**
     * This event's purpose is to dequeue the sapling block in the front of the queue every tick and
     * apply bone meal to it
     */
    public void pollBlockToBoneMeal() {
        SaplingRef ref = blocksToBeBoneMealed.poll();
        if (ref == null) return;
        queuedSaplings.remove(ref);

        World world = Bukkit.getWorld(ref.worldId());
        if (world == null) return;

        // Chunk-safe check
        if (!world.isChunkLoaded(ref.x() >> 4, ref.z() >> 4)) {
            return;
        }

        Block block = world.getBlockAt(ref.x(), ref.y(), ref.z());
        if (getTreeType(block) != null) {
            block.applyBoneMeal(BlockFace.UP);
        }
    }
}
