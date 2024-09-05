package me.mykindos.betterpvp.progression.profession.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Data;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.event.PlayerChopLogEvent;
import me.mykindos.betterpvp.progression.profession.woodcutting.leaderboards.TotalLogsChoppedLeaderboard;
import me.mykindos.betterpvp.progression.profession.woodcutting.repository.WoodcuttingRepository;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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


    /**
     * Maps the log type (key) to its base experience value for chopping it (value)
     */
    private Map<Material, Long> experiencePerWood;


    /**
     * Weighed collection containing every loot type that can drop for the Woodcutting profession
     */
    private WeighedList<WoodcuttingLootType> lootTypes;

    @Inject
    public WoodcuttingHandler(Progression progression, ProfessionProfileManager professionProfileManager, WoodcuttingRepository woodcuttingRepository, LeaderboardManager leaderboardManager) {
        super(progression, professionProfileManager, "Woodcutting");
        this.woodcuttingRepository = woodcuttingRepository;
        this.leaderboardManager = leaderboardManager;
    }


    /**
     * @param material The (type of) wood material that was mined by the player
     * @return The experience gained from mining said wood material
     */
    public long getExperienceFor(Material material) {
        return experiencePerWood.getOrDefault(material, 0L);
    }


    /**
     * Utility method used to determine whether a player placed a <code>block</code>
     * @param block the block in question
     * @return a boolean determining whether the player placed that block
     */
    public boolean didPlayerPlaceBlock(Block block) {
        return UtilBlock.getPersistentDataContainer(block).has(CoreNamespaceKeys.PLAYER_PLACED_KEY);
    }


    /**
     * This handles all the experience gaining and logging that happens when a
     * player chops a log (`block`)
     * @param chopLogEvent the event that was called
     * @param experienceModifier represents a higher order function that modifies
     *                           the experience gained by the player here.
     */
    public void attemptToChopLog(PlayerChopLogEvent chopLogEvent, DoubleUnaryOperator experienceModifier) {

        Player player = chopLogEvent.getPlayer();
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
        if (didPlayerPlaceBlock(block) && !chopLogEvent.isForestFlourisherTree()) {
            professionData.grantExperience(0, player);
            return;
        }

        professionData.grantExperience(finalExperience, player);
        woodcuttingRepository.saveChoppedLog(player.getUniqueId(), block.getType(), player.getLocation());

        log.info("{} chopped {} for {} experience", player.getName(), originalBlockType, finalExperience)
                .addClientContext(player).addBlockContext(block).addLocationContext(block.getLocation())
                .addContext("Experience", finalExperience + "").submit();

        long logsChopped = (long) professionData.getProperties().getOrDefault("TOTAL_LOGS_CHOPPED", 0L);
        professionData.getProperties().put("TOTAL_LOGS_CHOPPED", logsChopped + ((long) amountChopped));



        ItemStack toolUsed = chopLogEvent.getToolUsed();
        ItemMeta toolUsedMeta = toolUsed.getItemMeta();

        if (amountChopped > 1 && (toolUsedMeta instanceof Damageable metaAsDamageable)) {
            // Tree Feller is supposed to work only w/ axes so no need to check

            // the way unbreaking rune works is by cancelling the whole event, not
            // just reduction so that's why originalDamage will always equal damage
            PlayerItemDamageEvent playerItemDamageEvent = UtilServer.callEvent(
                    new PlayerItemDamageEvent(player, toolUsed, amountChopped, amountChopped)
            );

            if (!playerItemDamageEvent.isCancelled()) {
                metaAsDamageable.setDamage(amountChopped);
                toolUsed.setItemMeta(toolUsedMeta);
            }
        }

        // Checking if >0 is probably not necessary, but it's here for clarity
        int additionalLogsDropped = chopLogEvent.getAdditionalLogsDropped();
        if (additionalLogsDropped > 0) {
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(originalBlockType, additionalLogsDropped));
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
     * Represents a type of loot one can obtain from the *Woodcutting* profession
     */
    @Data
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

        lootTypes = new WeighedList<>();
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

            WoodcuttingLootType lootType = new WoodcuttingLootType(
                    material, customModelData, minAmount, maxAmount
            );

            lootTypes.add(frequency, 1, lootType);
        }

        log.info("Loaded " + lootTypes.size() + " woodcutting loot types").submit();
    }
}
