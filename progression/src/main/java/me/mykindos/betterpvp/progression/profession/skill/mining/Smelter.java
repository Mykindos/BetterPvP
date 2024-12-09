package me.mykindos.betterpvp.progression.profession.skill.mining;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.mining.event.PlayerMinesOreEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@Singleton
@BPvPListener
public class Smelter extends MiningProgressionSkill implements Listener {
    private final ProfessionProfileManager professionProfileManager;
    private double smeltChance;

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
        return new String[]{
                "Increases the chance of automatically smelting mined ores by <green>" + UtilMath.round(getSmeltChance(level) * 100, 1) + "%"
                        + "<reset> when using a diamond pickaxe or higher"
        };
    }

    @Override
    public Material getIcon() {
        return Material.BLAST_FURNACE;
    }

    public double getSmeltChance(int level) {
        return level * smeltChance;
    }


    @EventHandler
    public void onBlockBreak(PlayerMinesOreEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Material blockType = event.getMinedOreBlock().getType();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (!(itemInHand.getType() == Material.DIAMOND_PICKAXE || itemInHand.getType() == Material.NETHERITE_PICKAXE || itemInHand.getType() == Material.MUSIC_DISC_WARD))
            return;
        if (!UtilBlock.isRawOre(blockType)) return;

        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getPlayerSkillLevel(profile);
            if (skillLevel <= 0) return;
            if (UtilMath.randDouble(0.0, 1.0) > getSmeltChance(skillLevel)) return;

            switch (blockType) {
                case STONE:
                    event.setSmelted(Material.STONE, 1);
                    break;
                case IRON_ORE:
                case DEEPSLATE_IRON_ORE:
                    event.setSmelted(Material.IRON_INGOT, 1);
                    break;
                case GOLD_ORE:
                case DEEPSLATE_GOLD_ORE:
                    event.setSmelted(Material.GOLD_INGOT, 1);
                    break;
                case COPPER_ORE:
                case DEEPSLATE_COPPER_ORE:
                    int amount = event.getMinedOreBlock().getDrops().stream()
                            .mapToInt(ItemStack::getAmount)
                            .sum();
                    event.setSmelted(Material.COPPER_INGOT, amount);
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        smeltChance = getConfig("smeltChanceIncreasePerLvl", 0.01, Double.class);
    }
}
