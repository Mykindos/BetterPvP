package me.mykindos.betterpvp.progression.profession.skill.mining;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.framework.economy.CoinItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.progression.profession.mining.event.PlayerMinesOreEvent;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkillNode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@Singleton
@BPvPListener
public class GoldRush extends ProfessionSkillNode implements Listener {

    @Inject
    private BlockTagManager blockTagManager;

    private double goldChance;
    private int minCoinsFound;
    private int maxCoinsFound;

    @Inject
    public GoldRush(String name) {
        super("Gold Rush");

    }

    @Override
    public String getName() {
        return "Gold Rush";
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


    @EventHandler
    public void onBlockBreak(PlayerMinesOreEvent event) {
        Player player = event.getPlayer();
        Block block = event.getMinedOreBlock();
        Material blockType = block.getType();
        if (!UtilBlock.isOre(blockType)) return;

        blockTagManager.isPlayerManipulated(block).thenAcceptAsync(isPlayerManipulated -> {
            if (isPlayerManipulated) return;
            professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
                int skillLevel = getPlayerNodeLevel(profile);
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
    public void loadConfig() {
        super.loadConfig();
        goldChance = getConfig("coinsChanceIncreasePerLvl", 0.04, Double.class);
        minCoinsFound = getConfig("minCoinsFound", 100, Integer.class);
        maxCoinsFound = getConfig("maxCoinsFound", 5000, Integer.class);
    }

}
