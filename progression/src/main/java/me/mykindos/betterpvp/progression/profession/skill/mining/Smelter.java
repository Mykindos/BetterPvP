package me.mykindos.betterpvp.progression.profession.skill.mining;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

@Singleton
@BPvPListener
public class Smelter extends MiningProgressionSkill implements Listener {
    ProfessionProfileManager professionProfileManager;
    private double smeltChance;
    private static final Material[] validPickaxes = new Material[]{
            Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE,
            Material.NETHERITE_PICKAXE,
            Material.FIREWORK_STAR
    };

    @Inject
    public Smelter(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
    }

    @Override
    public String getName() {
        return "Smelter";
    }


    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Increases the chance of automatically smelting mined ores by <green>" + UtilMath.round(getSmeltChance(level), 2) + "%"
        };
    }

    @Override
    public Material getIcon() {
        return Material.BLAST_FURNACE;
    }

    public double getSmeltChance(int level) {
        return level * smeltChance;
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if(!isValidPickaxe(itemInHand.getType())) return;
        if(!UtilBlock.isRawOre(blockType)) return;

        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getPlayerSkillLevel(profile);
            if (skillLevel <= 0) return;
            if (UtilMath.randDouble(0.0, 1.0) > getSmeltChance(skillLevel)) return;

            switch (blockType) {
                case STONE:
                    event.setDropItems(false);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.STONE));
                    break;
                case IRON_ORE:
                case DEEPSLATE_IRON_ORE:
                    event.setDropItems(false);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.IRON_INGOT));
                    break;
                case GOLD_ORE:
                case DEEPSLATE_GOLD_ORE:
                    event.setDropItems(false);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.GOLD_INGOT));
                    break;
                case COPPER_ORE:
                case DEEPSLATE_COPPER_ORE:
                    int amount = event.getBlock().getDrops().stream()
                            .mapToInt(ItemStack::getAmount)
                            .sum();
                    event.setDropItems(false);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.COPPER_INGOT, amount));
                    break;
                default:
                    break;
            }
        });
    }

    private boolean isValidPickaxe(Material material) {
        return Arrays.asList(validPickaxes).contains(material);
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        smeltChance = getConfig("smeltChanceIncreasePerLvl", 0.01, Double.class);
    }
}
