package me.mykindos.betterpvp.progression.profession.skill.mining;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.economy.CoinItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.mining.event.PlayerMinesOreEvent;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillDependency;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

@Singleton
@BPvPListener
public class GoldRush extends MiningProgressionSkill implements Listener {
    private final ProfessionProfileManager professionProfileManager;
    private double goldChance;
    private int minCoinsFound;
    private int maxCoinsFound;

    @Inject
    public GoldRush(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
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

        final boolean playerPlaced = UtilBlock.isPlayerPlaced(block);

        if (playerPlaced) return;
        if(!UtilBlock.isOre(blockType)) return;

        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getPlayerSkillLevel(profile);
            if (skillLevel <= 0) return;
            if (UtilMath.randDouble(0.0, 100.0) > getCoinsChance(skillLevel)) return;

            //TODO: Maybe add multiplier during mining madness
            int coinsFound = UtilMath.randomInt(minCoinsFound, maxCoinsFound);

            ItemStack droppedCoins = CoinItem.SMALL_NUGGET.generateItem(coinsFound);
            block.getWorld().dropItemNaturally(block.getLocation(), droppedCoins);
        });
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        goldChance = getConfig("coinsChanceIncreasePerLvl", 0.04, Double.class);
        minCoinsFound = getConfig("minCoinsFound", 100, Integer.class);
        maxCoinsFound = getConfig("maxCoinsFound", 5000, Integer.class);
    }

    @Override
    public ProgressionSkillDependency getDependencies() {
        final String[] dependencies = new String[]{"Smelter"};
        return new ProgressionSkillDependency(dependencies, 250);
    }
}
