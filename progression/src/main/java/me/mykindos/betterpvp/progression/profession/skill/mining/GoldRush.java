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
                "Increases the chance of finding coins by <green>" + UtilMath.round(getCoinsChance(level) * 100, 2) + "%"
                        + "<reset> when mining ores"
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

        final PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(block);
        final boolean playerPlaced = pdc.has(CoreNamespaceKeys.PLAYER_PLACED_KEY);

        if (playerPlaced) return;
        if(!UtilBlock.isOre(blockType)) return;

        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getPlayerSkillLevel(profile);
            if (skillLevel <= 0) return;
            if (UtilMath.randDouble(0.0, 1.0) > getCoinsChance(skillLevel)) return;

            //TODO: Maybe add multiplier during mining madness
            int coinsFound = (int) Math.ceil(Math.pow((double) UtilMath.randomInt(minCoinsFound, maxCoinsFound) / maxCoinsFound, 2) * maxCoinsFound);

            CoinItem coinItem = CoinItem.SMALL_NUGGET;
            if (coinsFound >= 20000) {
                coinItem = CoinItem.BAR;
            } else if (coinsFound >= 5000) {
                coinItem = CoinItem.LARGE_NUGGET;
            }

            ItemStack droppedCoins = coinItem.generateItem(coinsFound);
            block.getWorld().dropItemNaturally(block.getLocation(), droppedCoins);
        });
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        goldChance = getConfig("coinsChanceIncreasePerLvl", 0.025, Double.class);
        minCoinsFound = getConfig("minCoinsFound", 100, Integer.class);
        maxCoinsFound = getConfig("maxCoinsFound", 50000, Integer.class);
    }
}
