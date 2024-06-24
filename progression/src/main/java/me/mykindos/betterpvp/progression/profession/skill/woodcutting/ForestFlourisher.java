package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillDependency;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Forest Flourisher is a 2-part skill with its event-handling being in the `clans`
 * plugin
 */
@Singleton
@BPvPListener
public class ForestFlourisher extends WoodcuttingProgressionSkill implements Listener {
    private final ProfessionProfileManager professionProfileManager;
    private double baseGrowTime;
    private double growTimeDecreasePerLvl;


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
                "Saplings you plant grow faster taking only <green>" + growTime(level) + "</green> seconds to regrow"
        };
    }

    public long growTime(int level) {
        return (long) (baseGrowTime - (growTimeDecreasePerLvl*level));
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

    /**
     * purpose of this method:
     * 1. check if a sapling is placed -- if so?
     * 2. it'll spawn this neat ring of cascading cherry leaf particles around the placed sapling
     * 3. It'll generate a new tree (between 30-90 seconds; depends on skillLevel)
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

            // purpose of the 0.5's is to put particle in center of block
            Location center = event.getBlock().getLocation().add(0.5, 1.5, 0.5);
            final int particleCount = 20;
            final double radius = 0.75;
            final double decreaseInYLvlPerParticle = 0.05;

            for (int i = 0; i < particleCount; i++) {
                double angle = 2 * Math.PI * i / particleCount;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);

                Location particleLocation = new Location(center.getWorld(), x, center.getY() - i*decreaseInYLvlPerParticle, z);

                UtilServer.runTaskLater(JavaPlugin.getPlugin(Progression.class), () -> {
                    event.getBlock().getWorld().spawnParticle(Particle.CHERRY_LEAVES, particleLocation, 0, 0, -1, 0);
                }, i);
            }

            UtilServer.runTaskLater(JavaPlugin.getPlugin(Progression.class), () -> {
                Block block = event.getBlock();

                if (getTreeType(block) == null) return;

                block.setType(Material.AIR);

                // if the player is in creative then this if statement probably won't have to trigger
                final PersistentDataContainer persistentDataContainer = UtilBlock.getPersistentDataContainer(block);
                if (persistentDataContainer.has(CoreNamespaceKeys.PLAYER_PLACED_KEY)) {
                    persistentDataContainer.remove(CoreNamespaceKeys.PLAYER_PLACED_KEY);
                    UtilBlock.setPersistentDataContainer(block, persistentDataContainer);
                }

                block.getWorld().generateTree(block.getLocation(), treeType);
            }, growTime(skillLevel) * 20L);
        });
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        baseGrowTime = getConfig("baseGrowTime", 90.0, Double.class);
        growTimeDecreasePerLvl = getConfig("growTimeDecreasePerLvl", 2.0, Double.class);
    }

    @Override
    public ProgressionSkillDependency getDependencies() {
        final String[] dependencies = new String[]{"Tree Feller"};
        return new ProgressionSkillDependency(dependencies, 20);
    }
}
