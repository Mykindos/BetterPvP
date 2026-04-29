package me.mykindos.betterpvp.progression.profession.skill.mining.buriedcache;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import me.mykindos.betterpvp.core.loot.session.LootSessionController;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.profession.mining.event.PlayerMinesOreEvent;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import me.mykindos.betterpvp.progression.profession.skill.mining.attributes.treasuretrigger.TreasureTriggerAttribute;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@Singleton
@NodeId("buried_cache")
public class BuriedCache extends ProfessionSkill {

    @Inject
    private LootTableRegistry lootTableRegistry;

    @Inject
    private LootSessionController sessionController;

    @Inject
    private TreasureTriggerAttribute treasureTrigger;

    @Inject
    private BlockTagManager blockTagManager;

    private double baseChance;
    private double chancePerLevel;
    private LootTable lootTable;

    @Inject
    public BuriedCache() {
        super("Buried Cache");
    }

    @Override
    public String[] getDescription(int level) {
        double chance = UtilMath.round(getTriggerChance(level) * 100.0, 2);
        return new String[]{
                "When mining stone-based blocks, you have a",
                "<green>" + chance + "% <reset>chance to uncover a buried cache",
                "filled with valuable loot.",
                "",
                "Does not work on player-placed blocks."
        };
    }

    @Override
    public Material getIcon() {
        return Material.CHEST;
    }

    public double getTriggerChance(int level) {
        return baseChance + (level * chancePerLevel);
    }

    public void onBlockBreak(PlayerMinesOreEvent event) {
        Player player = event.getPlayer();
        Block block = event.getMinedOreBlock();

        if (!UtilBlock.isStoneBased(block)) return;
        if (blockTagManager.isPlayerPlaced(block)) return;

        profileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            int skillLevel = getSkillLevel(profile);
            if (skillLevel <= 0) return;

            double chance = getTriggerChance(skillLevel) + treasureTrigger.getBonusChance(player);
            if (Math.random() >= chance) return;

            final LootTable table = lootTable;
            UtilServer.runTaskLater(getProgression(), () -> {
                block.setType(Material.CHEST);
                LootSession session = sessionController.resolve(player, table, () -> LootSession.newSession(table, player));
                LootContext context = new LootContext(session, block.getLocation(), "Mining");
                LootBundle bundle = table.generateLoot(context);
                BuriedCacheChestStrategy.fillChest(block, bundle, context);
            }, 1L);
        });
    }

    @Override
    public void loadSkillConfig() {
        baseChance = getSkillConfig("baseChance", 0.01, Double.class);
        chancePerLevel = getSkillConfig("chancePerLevel", 0.0005, Double.class);
        lootTable = lootTableRegistry.loadLootTable("mining_buried_cache");
    }
}
