package me.mykindos.betterpvp.progression.profession.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.loot.item.DroppedItemLoot;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import me.mykindos.betterpvp.core.loot.session.LootSessionController;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profession.woodcutting.leaderboards.TotalLogsChoppedLeaderboard;
import me.mykindos.betterpvp.progression.profession.woodcutting.repository.WoodcuttingRepository;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;


/**
 * This class's purpose is to listen for whenever a block is broken
 * and notify the WoodcuttingHandler appropriately.
 */
@Singleton
@CustomLog
@Getter
public class WoodcuttingHandler extends ProfessionHandler implements Reloadable {

    private final WoodcuttingRepository woodcuttingRepository;
    private final LeaderboardManager leaderboardManager;
    private final BlockTagManager blockTagManager;
    private final EffectManager effectManager;
    private final ItemFactory itemFactory;

    /**
     * Maps the log type (key) to its base experience value for chopping it (value)
     */
    private Map<Material, Long> experiencePerWood;

    private final LootTableRegistry lootTableRegistry;
    private final LootSessionController sessionController;
    private final Map<String, LootTable> logLootTables = new HashMap<>();
    private LootTable lootTable;

    @Inject
    public WoodcuttingHandler(Progression progression, ClientManager clientManager, ProfessionProfileManager professionProfileManager,
                              Provider<ProfessionNodeManager> nodeManager,
                              WoodcuttingRepository woodcuttingRepository, LeaderboardManager leaderboardManager,
                              BlockTagManager blockTagManager, EffectManager effectManager, ItemFactory itemFactory,
                              LootTableRegistry lootTableRegistry, LootSessionController sessionController) {
        super(progression, clientManager, professionProfileManager, nodeManager, "Woodcutting");
        this.woodcuttingRepository = woodcuttingRepository;
        this.leaderboardManager = leaderboardManager;
        this.blockTagManager = blockTagManager;
        this.effectManager = effectManager;
        this.itemFactory = itemFactory;
        this.lootTableRegistry = lootTableRegistry;
        this.sessionController = sessionController;
    }

    /**
     * Reloads the configuration or state of this class.
     */
    @Override
    public void reload() {
        this.logLootTables.put("OAK_LOG", lootTableRegistry.loadLootTable("woodcutting_oak_log"));
        this.logLootTables.put("SPRUCE_LOG", lootTableRegistry.loadLootTable("woodcutting_spruce_log"));
        this.logLootTables.put("BIRCH_LOG", lootTableRegistry.loadLootTable("woodcutting_birch_log"));
        this.logLootTables.put("JUNGLE_LOG", lootTableRegistry.loadLootTable("woodcutting_jungle_log"));
        this.logLootTables.put("ACACIA_LOG", lootTableRegistry.loadLootTable("woodcutting_acacia_log"));
        this.logLootTables.put("DARK_OAK_LOG", lootTableRegistry.loadLootTable("woodcutting_dark_oak_log"));
        this.logLootTables.put("MANGROVE_LOG", lootTableRegistry.loadLootTable("woodcutting_mangrove_log"));
        this.logLootTables.put("CHERRY_LOG", lootTableRegistry.loadLootTable("woodcutting_cherry_log"));
        this.logLootTables.put("PALE_OAK_LOG", lootTableRegistry.loadLootTable("woodcutting_pale_oak_log"));
        this.logLootTables.put("CRIMSON_STEM", lootTableRegistry.loadLootTable("woodcutting_crimson_stem"));
        this.logLootTables.put("WARPED_STEM", lootTableRegistry.loadLootTable("woodcutting_warped_stem"));

        // stripped logs
        this.logLootTables.put("STRIPPED_OAK_LOG", lootTableRegistry.loadLootTable("woodcutting_stripped_oak_log"));
        this.logLootTables.put("STRIPPED_SPRUCE_LOG", lootTableRegistry.loadLootTable("woodcutting_stripped_spruce_log"));
        this.logLootTables.put("STRIPPED_BIRCH_LOG", lootTableRegistry.loadLootTable("woodcutting_stripped_birch_log"));
        this.logLootTables.put("STRIPPED_JUNGLE_LOG", lootTableRegistry.loadLootTable("woodcutting_stripped_jungle_log"));
        this.logLootTables.put("STRIPPED_ACACIA_LOG", lootTableRegistry.loadLootTable("woodcutting_stripped_acacia_log"));
        this.logLootTables.put("STRIPPED_DARK_OAK_LOG", lootTableRegistry.loadLootTable("woodcutting_stripped_dark_oak_log"));
        this.logLootTables.put("STRIPPED_MANGROVE_LOG", lootTableRegistry.loadLootTable("woodcutting_stripped_mangrove_log"));
        this.logLootTables.put("STRIPPED_CHERRY_LOG", lootTableRegistry.loadLootTable("woodcutting_stripped_cherry_log"));
        this.logLootTables.put("STRIPPED_PALE_OAK_LOG", lootTableRegistry.loadLootTable("woodcutting_stripped_pale_oak_log"));
        this.lootTable = lootTableRegistry.loadLootTable("woodcutting");
    }

    /**
     * @param material The (type of) wood material that was mined by the player
     * @return The experience gained from mining said wood material
     */
    public long getExperienceFor(Material material) {
        return experiencePerWood.getOrDefault(material, 0L);
    }

    /**
     * This handles all the experience gaining and logging that happens when a
     * player chops a log (`block`)
     *
     * @param chopLogEvent       the event that was called
     * @param experienceModifier represents a higher order function that modifies
     *                           the experience gained by the player here.
     */
    public void attemptToChopLog(PlayerChopLogEvent chopLogEvent, DoubleUnaryOperator experienceModifier) {

        final Player player = chopLogEvent.getPlayer();
        ProfessionData professionData = getProfessionData(player.getUniqueId());
        if (professionData == null) {
            return;
        }

        Material originalBlockType = chopLogEvent.getLogType();
        long experience = getExperienceFor(originalBlockType);
        if (experience <= 0) {
            return;
        }

        int amountChopped = chopLogEvent.getAmountChopped();
        final double finalExperience = experienceModifier.applyAsDouble(experience) * amountChopped;

        Block block = chopLogEvent.getChoppedLogBlock();
        if (blockTagManager.isPlayerPlaced(block)) {
            professionData.grantExperience(0, player);
            return;
        }

        professionData.grantExperience(finalExperience, player);
        woodcuttingRepository.saveChoppedLog(player, block.getType(), player.getLocation(), chopLogEvent.getAmountChopped());

        log.info("{} chopped {} for {} experience", player.getName(), originalBlockType, finalExperience)
                .addClientContext(player).addBlockContext(block).addLocationContext(block.getLocation())
                .addContext("Experience", finalExperience + "").submit();

        long logsChopped = (long) professionData.getProperties().getOrDefault("TOTAL_LOGS_CHOPPED", 0L);
        professionData.getProperties().put("TOTAL_LOGS_CHOPPED", logsChopped + ((long) amountChopped));

        clientManager.incrementStat(player, ClientStat.LOG_CHOPPED, (long) amountChopped);

        ItemStack toolUsed = chopLogEvent.getToolUsed();

        if (amountChopped > 1) {
            UtilItem.damageItem(player, toolUsed, amountChopped);
        }

        boolean isProtected = effectManager.hasEffect(player, EffectTypes.PROTECTION);
        // Checking if >0 is probably not necessary, but it's here for clarity
        int additionalLogsDropped = chopLogEvent.getAdditionalLogsDropped();
        if (additionalLogsDropped > 0) {
            Item item = block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(originalBlockType, additionalLogsDropped));
            if (isProtected) {
                UtilItem.reserveItem(item, player, 10);
            }
        }

        leaderboardManager.getObject("Total Logs Chopped").ifPresent(leaderboard -> {
            TotalLogsChoppedLeaderboard totalLogsChoppedLeaderboard = (TotalLogsChoppedLeaderboard) leaderboard;

            // the purpose of this line is increment the value on the leaderboard
            totalLogsChoppedLeaderboard.add(player.getUniqueId(), ((long) amountChopped)).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to add chopped logs to leaderboard for player " + player.getName(), throwable).submit();
                }

                if(true) {
                    return;
                }

                totalLogsChoppedLeaderboard.attemptAnnounce(player, result);
            });
        });
    }

    @Override
    public String getName() {
        return "Woodcutting";
    }

    public void processLogDropReplacement(Player player, Block block) {
        boolean isProtected = effectManager.hasEffect(player, EffectTypes.PROTECTION);
        LootTable logLootTable = logLootTables.get(block.getType().name());
        LootSession lootSession = sessionController.resolve(player, logLootTable, () -> LootSession.newSession(logLootTable, player));
        LootContext lootContext = new LootContext(lootSession, block.getLocation(), "Woodcutting");
        LootBundle loot = logLootTable.generateLoot(lootContext);

        loot.getLoot().stream().filter(lootType -> lootType instanceof DroppedItemLoot)
                .forEach(lootType -> {
                    DroppedItemLoot droppedItemLoot = (DroppedItemLoot) lootType;
                    Item item = droppedItemLoot.award(lootContext);
                    if (isProtected) {
                        UtilItem.reserveItem(item, player, 10);
                    }
                });
    }

    /**
     * This function will try to get a configuration section for the path but if there is none, it will create
     * a new section at path
     */
    private ConfigurationSection createOrGetSection(ConfigurationSection parentSection, String path) {
        ConfigurationSection section = parentSection.getConfigurationSection(path);
        return section != null ? section : parentSection.createSection(path);
    }

    /**
     * - Loads the YAML configuration for the Woodcutting Profession
     * - This function will cause side effects by logging messages to the console
     */
    public void loadConfig() {
        super.loadConfig();

        var config = progression.getConfig();


        ConfigurationSection woodcuttingSection = createOrGetSection(config, "woodcutting");

        experiencePerWood = new EnumMap<>(Material.class);
        ConfigurationSection experienceSection = createOrGetSection(woodcuttingSection, "experiencePerWood");

        for (String materialAsKey : experienceSection.getKeys(false)) {

            Material woodLogMaterial = Material.getMaterial(materialAsKey.toUpperCase());
            if (woodLogMaterial == null) continue;

            long experienceGiven = experienceSection.getLong(materialAsKey);
            experiencePerWood.put(woodLogMaterial, experienceGiven);
        }

        log.info("Loaded " + experiencePerWood.size() + " woodcutting blocks").submit();
    }
}
