package me.mykindos.betterpvp.progression.profession.skill.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillDependency;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

/**
 * This perk grants a chance for the player to receive a <i>special item</i> when they strip a log.
 * <p>
 *     This item is <b>Tree Bark</b> which can sold at the shops
 * </p>
 */
@Singleton
@BPvPListener
public class BarkBounty extends WoodcuttingProgressionSkill implements Listener {
    ProfessionProfileManager professionProfileManager;
    WoodcuttingHandler woodcuttingHandler;

    /**
     * Represents the percentage, per level, that <b>Tree Bark</b> will drop when any given log is stripped
     */
    private double barkChanceIncreasePerLvl;

    /**
     * Represents the log types that can be stripped in order to obtain <b>Tree Bark</b>
     */
    private final Material[] validLogTypes = new Material[]{
            Material.OAK_LOG,
            Material.ACACIA_LOG,
            Material.BIRCH_LOG,
            Material.DARK_OAK_LOG,
            Material.JUNGLE_LOG,
            Material.SPRUCE_LOG
    };

    /**
     * Represents the axe types that can be used to strip logs
     */
    private final Material[] validAxeTypes = new Material[]{
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE
    };

    @Inject
    public BarkBounty(Progression progression, ProfessionProfileManager professionProfileManager, WoodcuttingHandler woodcuttingHandler) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
        this.woodcuttingHandler = woodcuttingHandler;
    }

    @Override
    public String getName() {
        return "Bark Bounty";
    }

    @Override
    public String[] getDescription(int level) {
        double numberInPercentage = getChanceForBarkToDrop(level) * 100;
        return new String[] {
                "When you strip a log, there is <green>" + numberInPercentage + "%</green> to drop <aqua>Tree Bark</aqua>",
                "",
                "<aqua>Tree Bark</aqua> can be sold at shops"
        };
    }

    @Override
    public Material getIcon() {
        return Material.STRIPPED_OAK_LOG;
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        barkChanceIncreasePerLvl = getConfig("barkChanceIncreasePerLvl ", 0.004, Double.class);
    }

    @Override
    public ProgressionSkillDependency getDependencies() {
        final String[] dependencies = new String[]{"Tree Feller"};
        return new ProgressionSkillDependency(dependencies, 20);
    }

    /**
     * Calculates the probability that Tree Bark will drop when a log is stripped
     * <p>
     * 0.0 < the value that is returned <= 1.0
     */
    public double getChanceForBarkToDrop(int level) {
        return barkChanceIncreasePerLvl * level;
    }

    /**
     * Whenever a player strips a log, the logic in this method <i>should</i> be executed, and this method will
     * get a random number to see if the player should receive <b>Tree Bark</b> based on their level
     */
    @EventHandler
    public void whenPlayerStripsALog(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!Arrays.asList(validLogTypes).contains(block.getType())) return;

        Player player = event.getPlayer();
        Material playerItemInHand = player.getInventory().getItemInMainHand().getType();
        if (!Arrays.asList(validAxeTypes).contains(playerItemInHand)) return;

        if (woodcuttingHandler.didPlayerPlaceBlock(block)) return;


        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getPlayerSkillLevel(profile);
            if (skillLevel <= 0) return;

            if (Math.random() < getChanceForBarkToDrop(skillLevel)) {
                ItemStack treeBark = new ItemStack(Material.GLISTERING_MELON_SLICE);

                player.getWorld().dropItemNaturally(player.getLocation(), treeBark);
            }
        });
    }
}