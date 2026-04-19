package me.mykindos.betterpvp.progression.profession.skill.mining.goldrush;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.framework.economy.CoinItem;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.progression.profession.mining.event.PlayerMinesOreEvent;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import me.mykindos.betterpvp.progression.profession.skill.SkillId;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Singleton
@SkillId("gold_rush")
public class GoldRush extends ProfessionSkill {

    @Inject
    private BlockTagManager blockTagManager;

    private double goldChance;
    private int minCoinsFound;
    private int maxCoinsFound;

    @Inject
    public GoldRush() {
        super("Gold Rush");
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Increases the chance of finding coins by <green>" + UtilMath.round(getCoinsChance(level), 3) + "%",
                "when mining ores",
                "",
                "Does not work at Fields."
        };
    }

    @Override
    public Material getIcon() {
        return Material.RAW_GOLD;
    }

    public double getCoinsChance(int level) {
        return level * goldChance;
    }


    public void onBlockBreak(PlayerMinesOreEvent event) {
        Player player = event.getPlayer();
        Block block = event.getMinedOreBlock();
        Material blockType = block.getType();
        if (!UtilBlock.isOre(blockType)) return;

        blockTagManager.isPlayerManipulated(block).thenAcceptAsync(isPlayerManipulated -> {
            if (isPlayerManipulated) return;
            profileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
                int skillLevel = getSkillLevel(profile);
                if (skillLevel <= 0) return;
                if (UtilMath.randDouble(0.0, 100.0) > getCoinsChance(skillLevel)) return;

                //TODO: Maybe add multiplier during mining madness
                int coinsFound = UtilMath.randomInt(minCoinsFound, maxCoinsFound);

                ItemStack droppedCoins = CoinItem.SMALL_NUGGET.generateItem(coinsFound);
                block.getWorld().dropItemNaturally(block.getLocation(), droppedCoins);
            });
        }, Bukkit.getScheduler().getMainThreadExecutor(getProgression()));


    }

    @Override
    public void loadSkillConfig() {
        
        goldChance = getSkillConfig("coinsChanceIncreasePerLvl", 0.04, Double.class);
        minCoinsFound = getSkillConfig("minCoinsFound", 100, Integer.class);
        maxCoinsFound = getSkillConfig("maxCoinsFound", 5000, Integer.class);
    }

}
