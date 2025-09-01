package me.mykindos.betterpvp.progression.profession.woodcutting;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.droptables.DropTable;
import me.mykindos.betterpvp.core.droptables.DropTableItemStack;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profession.woodcutting.leaderboards.TotalLogsChoppedLeaderboard;
import me.mykindos.betterpvp.progression.profession.woodcutting.repository.WoodcuttingRepository;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;


/**
 * This class's purpose is to listen for whenever a block is broken
 * and notify the WoodcuttingHandler appropriately.
 */
@Singleton
@CustomLog
@Getter
public class WoodcuttingHandler extends ProfessionHandler {


    private final WoodcuttingRepository woodcuttingRepository;
    private final LeaderboardManager leaderboardManager;
    private final BlockTagManager blockTagManager;
    private final EffectManager effectManager;
    private final ItemFactory itemFactory;

    /**
     * Maps the log type (key) to its base experience value for chopping it (value)
     */
    private Map<Material, Long> experiencePerWood;


    /**
     * DropTable containing every loot type that can drop for the Woodcutting profession
     */
    private DropTable lootTypes;

    @Inject
    public WoodcuttingHandler(Progression progression, ClientManager clientManager, ProfessionProfileManager professionProfileManager,
                              WoodcuttingRepository woodcuttingRepository, LeaderboardManager leaderboardManager,
                              BlockTagManager blockTagManager, EffectManager effectManager, ItemFactory itemFactory) {
        super(progression, clientManager, professionProfileManager, "Woodcutting");
        this.woodcuttingRepository = woodcuttingRepository;
        this.leaderboardManager = leaderboardManager;
        this.blockTagManager = blockTagManager;
        this.effectManager = effectManager;
        this.itemFactory = itemFactory;
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
     * @param chopLogEvent the event that was called
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
        woodcuttingRepository.saveChoppedLog(player.getUniqueId(), block.getType(), player.getLocation(), chopLogEvent.getAmountChopped());

        log.info("{} chopped {} for {} experience", player.getName(), originalBlockType, finalExperience)
                .addClientContext(player).addBlockContext(block).addLocationContext(block.getLocation())
                .addContext("Experience", finalExperience + "").submit();

        long logsChopped = (long) professionData.getProperties().getOrDefault("TOTAL_LOGS_CHOPPED", 0L);
        professionData.getProperties().put("TOTAL_LOGS_CHOPPED", logsChopped + ((long) amountChopped));

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

                totalLogsChoppedLeaderboard.attemptAnnounce(player, result);
            });
        });
    }

    @Override
    public String getName() {
        return "Woodcutting";
    }

    /**
     * Gets a random item from the loot table with a random amount between min and max
     * @return An ItemStack with a random amount
     */
    public ItemStack getRandomLoot() {
        DropTableItemStack item = lootTypes.random();
        if (item == null) return null;

        return item.create();
    }

    /**
     * Represents a type of loot one can obtain from the *Woodcutting* profession
     * @deprecated Use DropTable instead
     */
    @Data
    @Deprecated
    public static class WoodcuttingLootType {
        private final Material material;
        private final int customModelData;
        private final int minAmount;
        private final int maxAmount;
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

        lootTypes = new DropTable("woodcutting", "main");
        ConfigurationSection lootSection = createOrGetSection(woodcuttingSection, "loot");

        for (String lootItemKey : lootSection.getKeys(false)) {

            ConfigurationSection lootItemSection = lootSection.getConfigurationSection(lootItemKey);
            if (lootItemSection == null) continue;

            String itemMaterialAsString = lootItemSection.getString("material");
            if (itemMaterialAsString == null) continue;

            Material material = Material.getMaterial(itemMaterialAsString.toUpperCase());
            if (material == null) continue;

            int customModelData = lootItemSection.getInt("customModelData");
            int frequency = lootItemSection.getInt("frequency");
            int minAmount = lootItemSection.getInt("minAmount");
            int maxAmount = lootItemSection.getInt("maxAmount");
            DropTableItemStack itemStack = null;
            if (lootItemKey.contains(":")) {
                final NamespacedKey key = NamespacedKey.fromString(lootItemKey);
                if (key == null) {
                    log.error("Invalid namespaced key for loot item: " + lootItemKey).submit();
                    continue;
                }

                final BaseItem baseItem = itemFactory.getItemRegistry().getItem(key);
                if (baseItem == null) {
                    log.warn("No item found for key: " + key).submit();
                } else {
                    itemStack = new DropTableItemStack(itemFactory.create(baseItem).createItemStack(), minAmount, maxAmount);
                }

            } else {
                Material item = Material.valueOf(itemMaterialAsString);
                itemStack = new DropTableItemStack(UtilItem.createItemStack(item, 1, customModelData), minAmount, maxAmount);
            }

            lootTypes.add(frequency, 1, itemStack);
        }

        log.info("Loaded " + lootTypes.size() + " woodcutting loot types").submit();
    }
}
