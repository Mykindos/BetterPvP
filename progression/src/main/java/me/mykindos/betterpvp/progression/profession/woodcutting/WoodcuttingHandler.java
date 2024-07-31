package me.mykindos.betterpvp.progression.profession.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.woodcutting.leaderboards.TotalLogsChoppedLeaderboard;
import me.mykindos.betterpvp.progression.profession.woodcutting.repository.WoodcuttingRepository;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;


/**
 * This class's purpose is to handle woodcutting operations like spawning extra logs and granting
 * players XP for woodcutting
 */
@Singleton
@CustomLog
@Getter
public class WoodcuttingHandler extends ProfessionHandler {
    private final WoodcuttingRepository woodcuttingRepository;
    private Map<Material, Long> experiencePerWood = new EnumMap<>(Material.class);
    private final LeaderboardManager leaderboardManager;

    private final WeighedList<WoodcuttingLoot> lootTypes = new WeighedList<>();

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

    public boolean didPlayerPlaceBlock(Block block) {
        return UtilBlock.getPersistentDataContainer(block).has(CoreNamespaceKeys.PLAYER_PLACED_KEY);
    }

    /**
     * This handles all the experience gaining and logging that happens when a
     * player chops a log (`block`)
     * @param experienceModifier represents a higher order function that modifies
     *                           the experience gained by the player here.
     */
    public void attemptToChopLog(Player player, Material originalBlockType, Block block, DoubleUnaryOperator experienceModifier,
                                 int amountChopped, int additionalLogsDropped) {
        ProfessionData professionData = getProfessionData(player.getUniqueId());
        if (professionData == null) {
            return;
        }

        long experience = getExperienceFor(originalBlockType);
        if (experience <= 0) {
            return;
        }

        final double finalExperience = experienceModifier.applyAsDouble(experience) * amountChopped;

        if (didPlayerPlaceBlock(block)) {
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

        // Checking if >0 is probably not necessary, but it's here for clarity
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

    @FunctionalInterface
    public interface VarargsFunction {
        String apply(String... args);
    }

    public void loadConfig() {
        super.loadConfig();

        // not entirely sure if this line is necessary
        experiencePerWood = new EnumMap<>(Material.class);
        var config = progression.getConfig();

        // Joins String inputs (args) together into one path with a base path of "woodcutting"
        // ex: path.apply("shinei", "nouzen", "8", "6") -> "woodcutting.shinei.nouzen.8.6"
        VarargsFunction path = (String... args) -> "woodcutting." + String.join(".", args);

        String pathForExperiencePerWood = path.apply("experiencePerWood");
        ConfigurationSection experienceSection = config.getConfigurationSection(pathForExperiencePerWood);

        if (experienceSection == null) {
            experienceSection = config.createSection(pathForExperiencePerWood);
        }

        for (String key : experienceSection.getKeys(false)) {
            Material woodLogMaterial = Material.getMaterial(key.toUpperCase());
            if (woodLogMaterial == null) continue;

            long experienceGiven = config.getLong(path.apply("experiencePerWood", key));
            experiencePerWood.put(woodLogMaterial, experienceGiven);
        }
        log.info("Loaded " + experiencePerWood.size() + " woodcutting blocks").submit();


        String pathForLoot = path.apply("loot");
        ConfigurationSection lootSection = config.getConfigurationSection(pathForLoot);

        if (lootSection == null) {
            lootSection = config.createSection(pathForLoot);
        }

        lootTypes.clear();

        for (String key : lootSection.getKeys(false)) {

            ConfigurationSection lootSectionData = lootSection.getConfigurationSection(key);
            if (lootSectionData == null) {
                lootSection.createSection(key);
            }

            String type = Objects.requireNonNull(lootSectionData).getString("type", "common");
            String material = Objects.requireNonNull(lootSectionData).getString("material", "STONE");
            int customModelData = Objects.requireNonNull(lootSectionData).getInt("customModelData", 0);
            int frequency = Objects.requireNonNull(lootSectionData).getInt("frequency", 1);
            int minAmount = Objects.requireNonNull(lootSectionData).getInt("minAmount", 1);
            int maxAmount = Objects.requireNonNull(lootSectionData).getInt("maxAmount", 1);

            WoodcuttingLootType woodcuttingLootType = WoodcuttingLootType.valueOf(type.toUpperCase());

            Material lootMaterial = Material.getMaterial(material.toUpperCase());
            if (lootMaterial == null) continue;

            WoodcuttingLoot woodcuttingLoot = new WoodcuttingLoot(
                    woodcuttingLootType, lootMaterial, customModelData, frequency, minAmount, maxAmount
            );
            lootTypes.add(frequency, woodcuttingLootType.getNumVal(), woodcuttingLoot);
        }

        log.info("Loaded " + lootTypes.size() + " woodcutting loot types").submit();
    }
}
