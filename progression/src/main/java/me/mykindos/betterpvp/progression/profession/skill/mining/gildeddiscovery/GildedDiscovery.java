package me.mykindos.betterpvp.progression.profession.skill.mining.gildeddiscovery;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.progression.profession.mining.event.PlayerMinesOreEvent;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import me.mykindos.betterpvp.progression.profession.skill.mining.attributes.goldenchance.GoldenChanceAttribute;
import me.mykindos.betterpvp.progression.profession.skill.mining.attributes.goldenyield.GoldenYieldAttribute;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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

    public void onBlockBreak(PlayerMinesOreEvent event) {
        Player player = event.getPlayer();
        Block block = event.getMinedOreBlock();

        if (!UtilBlock.isStoneBased(block)) return;
        if (UtilBlock.isOre(block.getType())) return;
        if (blockTagManager.isPlayerPlaced(block)) return;

        profileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getSkillLevel(profile);
            if (skillLevel <= 0) return;

            double chance = getTriggerChance(skillLevel) + goldenChance.getBonusChance(player);
            if (Math.random() >= chance) return;

            int extraYield = UtilMath.randomInt(baseYieldMin, baseYieldMax) + (int) goldenYield.getBonusYield(player);
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.GOLD_ORE, extraYield));
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
