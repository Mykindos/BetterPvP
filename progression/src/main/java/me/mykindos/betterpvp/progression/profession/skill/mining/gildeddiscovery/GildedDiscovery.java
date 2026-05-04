package me.mykindos.betterpvp.progression.profession.skill.mining.gildeddiscovery;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import me.mykindos.betterpvp.progression.profession.skill.mining.attributes.goldenchance.GoldenChanceAttribute;
import me.mykindos.betterpvp.progression.profession.skill.mining.attributes.goldenyield.GoldenYieldAttribute;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

@Singleton
@NodeId("gilded_discovery")
public class GildedDiscovery extends ProfessionSkill {

    @Inject
    private GoldenChanceAttribute goldenChance;

    @Inject
    private GoldenYieldAttribute goldenYield;

    @Inject
    private BlockTagManager blockTagManager;

    private double baseChance;
    private double chancePerLevel;
    private int baseYieldMin;
    private int baseYieldMax;

    @Inject
    public GildedDiscovery() {
        super("Gilded Discovery");
    }

    @Override
    public String[] getDescription(int level) {
        double chance = UtilMath.round(getTriggerChance(level) * 100.0, 2);
        return new String[]{
                "When mining stone-based blocks, you have a",
                "<green>" + chance + "% <reset>chance to discover a gold vein,",
                "dropping <green>" + baseYieldMin + "-" + baseYieldMax + " <reset>extra gold ore.",
                "",
                "Does not trigger on actual ore blocks or player-placed blocks."
        };
    }

    @Override
    public Material getIcon() {
        return Material.GOLD_ORE;
    }

    public double getTriggerChance(int level) {
        return baseChance + (level * chancePerLevel);
    }

    public void onBlockBreak(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        BlockState state = event.getBlockState();
        if (blockTagManager.isPlayerPlaced(event.getBlock())) return;

        if (!UtilBlock.isStoneBased(state.getType())) return;
        if (UtilBlock.isOre(state.getType())) return;

        profileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getSkillLevel(profile);
            if (skillLevel <= 0) return;

            double chance = getTriggerChance(skillLevel) + goldenChance.getBonusChance(player);
            if (Math.random() >= chance) return;

            int extraYield = UtilMath.randomInt(baseYieldMin, baseYieldMax) + (int) goldenYield.getBonusYield(player);
            final ItemStack stack = new ItemStack(Material.GOLD_ORE, extraYield);
            final Item item = state.getWorld().dropItemNaturally(state.getLocation().toCenterLocation(), stack);
            event.getItems().add(item);
        });
    }

    @Override
    public void loadSkillConfig() {
        baseChance = getSkillConfig("baseChance", 0.02, Double.class);
        chancePerLevel = getSkillConfig("chancePerLevel", 0.0005, Double.class);
        baseYieldMin = getSkillConfig("baseYieldMin", 1, Integer.class);
        baseYieldMax = getSkillConfig("baseYieldMax", 2, Integer.class);
    }
}
