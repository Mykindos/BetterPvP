package me.mykindos.betterpvp.progression.profession.skill.mining.buriedcache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
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
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.progression.profession.skill.NodeId;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkill;
import me.mykindos.betterpvp.progression.profession.skill.mining.attributes.treasuretrigger.TreasureTriggerAttribute;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.concurrent.TimeUnit;

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
    private double expirationSeconds;
    private LootTable lootTable;

    private Cache<Location, Block> activeCaches;

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

    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (!UtilBlock.isStoneBased(block.getType())) return;
        if (blockTagManager.isPlayerPlaced(event.getBlock())) return;

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
                activeCaches.put(block.getLocation(), block);
                playAppearanceEffect(block.getLocation());
            }, 1L);
        });
    }

    public boolean isActiveCache(Block block) {
        return activeCaches.getIfPresent(block.getLocation()) != null;
    }

    public void expireChest(Block block) {
        activeCaches.invalidate(block.getLocation());
    }

    private void onCacheRemoved(Block block, RemovalCause cause) {
        if (cause == RemovalCause.REPLACED) return;
        Bukkit.getScheduler().runTask(getProgression(), () -> {
            if (block.getType() != Material.CHEST) return;
            block.setType(Material.AIR);
            playAppearanceEffect(block.getLocation());
        });
    }

    private void playAppearanceEffect(Location location) {
        Location center = location.clone().add(0.5, 0.5, 0.5);
        new SoundEffect(Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.0f).play(center);
        Particle.CLOUD.builder()
                .location(center)
                .count(20)
                .offset(0.4, 0.4, 0.4)
                .extra(0.05)
                .receivers(48, true)
                .spawn();
    }

    @Override
    public void loadSkillConfig() {
        baseChance = getSkillConfig("baseChance", 0.01, Double.class);
        chancePerLevel = getSkillConfig("chancePerLevel", 0.0, Double.class);
        expirationSeconds = getSkillConfig("expirationSeconds", 30.0, Double.class);
        lootTable = lootTableRegistry.loadLootTable("mining_buried_cache");

        if (activeCaches != null) {
            activeCaches.invalidateAll();
        }
        activeCaches = Caffeine.newBuilder()
                .scheduler(Scheduler.systemScheduler())
                .expireAfterWrite((long) (expirationSeconds * 1000L), TimeUnit.MILLISECONDS)
                .removalListener((Location loc, Block block, RemovalCause cause) -> {
                    if (block != null) onCacheRemoved(block, cause);
                })
                .build();
    }
}
