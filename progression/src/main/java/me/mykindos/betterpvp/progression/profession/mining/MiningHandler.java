package me.mykindos.betterpvp.progression.profession.mining;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.ProfessionHandler;
import me.mykindos.betterpvp.progression.profession.mining.leaderboards.MiningOresMinedLeaderboard;
import me.mykindos.betterpvp.progression.profession.mining.repository.MiningRepository;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.LongUnaryOperator;

@Singleton
@CustomLog
@Getter
public class MiningHandler extends ProfessionHandler {

    private final MiningRepository miningRepository;
    private final LeaderboardManager leaderboardManager;

    private Map<Material, Long> experiencePerBlock = new EnumMap<>(Material.class);
    private Set<Material> leaderboardBlocks = new HashSet<>();

    @Inject
    public MiningHandler(Progression progression, ProfessionProfileManager professionProfileManager, MiningRepository miningRepository, LeaderboardManager leaderboardManager) {
        super(progression, professionProfileManager, "Mining");
        this.miningRepository = miningRepository;
        this.leaderboardManager = leaderboardManager;
    }

    public Set<Material> getLeaderboardBlocks() {
        return Collections.unmodifiableSet(leaderboardBlocks);
    }

    public String getDbMaterialsList() {
        return getLeaderboardBlocks().stream()
                .map(mat -> "'" + mat.name() + "'")
                .reduce((a, b) -> a + "," + b)
                .orElse("''");
    }

    public long getExperienceFor(Material material) {
        return experiencePerBlock.getOrDefault(material, 0L);
    }

    public void attemptMineOre(Player player, Block block) {
        attemptMineOre(player, block, LongUnaryOperator.identity());
    }

    public void attemptMineOre(Player player, Block block, LongUnaryOperator experienceModifier) {

        ProfessionData professionData = getProfessionData(player.getUniqueId());
        if (professionData == null) return;

        long experience = getExperienceFor(block.getType());
        if (experience <= 0) {
            return; // Cancel if they don't get experience from this block
        }

        final long finalExperience = experienceModifier.applyAsLong(experience);

        // If it was player placed, grant 0 experience, so they get no XP and have a message sent anyway
        final PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(block);
        final boolean playerPlaced = pdc.has(CoreNamespaceKeys.PLAYER_PLACED_KEY);
        if (playerPlaced) {
            professionData.grantExperience(0, player);
            return;
        }

        // We give XP for any block that gives XP, even if it's not in the leaderboard
        professionData.grantExperience(finalExperience, player);
        //miningData.saveOreMined(block); // Save this block to the database

        log.info("{} mined {} for {} experience", player.getName(), block.getType(), finalExperience)
                .addClientContext(player).addBlockContext(block).addLocationContext(block.getLocation())
                .addContext("Experience", finalExperience + "").submit();

        if (!getLeaderboardBlocks().contains(block.getType())) {
            return;
        }

        int oresMined = (int) professionData.getProperties().getOrDefault("TOTAL_ORES_MINED", 0);
        professionData.getProperties().put("TOTAL_ORES_MINED", oresMined + 1);

        leaderboardManager.getObject("Total Ores Mined").ifPresent(leaderboard -> {
            MiningOresMinedLeaderboard oresMinedLeaderboard = (MiningOresMinedLeaderboard) leaderboard;
            oresMinedLeaderboard.add(player.getUniqueId(), 1L).whenComplete((result, throwable2) -> {
                if (throwable2 != null) {
                    log.error("Failed to add ore count to leaderboard for player " + player.getName(), throwable2).submit();
                    return;
                }

                oresMinedLeaderboard.attemptAnnounce(player, result);
            });
        });

    }

    @Override
    public String getName() {
        return "Mining";
    }

    public void loadConfig() {
        super.loadConfig();

        experiencePerBlock = new EnumMap<>(Material.class);

        var config = progression.getConfig();

        ConfigurationSection section = config.getConfigurationSection("mining.xpPerBlock");
        if (section == null) {
            section = config.createSection("mining.xpPerBlock");
        }

        for (String key : section.getKeys(false)) {
            final Material material = Material.getMaterial(key.toUpperCase());
            if (material == null) {
                continue;
            }

            experiencePerBlock.put(material, config.getLong("mining.xpPerBlock." + key));
        }
        log.info("Loaded " + experiencePerBlock.size() + " mining blocks").submit();

        final int minXpThreshold = config.getInt("mining.minLeaderboardBlockXp");

        leaderboardBlocks = new HashSet<>();
        leaderboardBlocks.addAll(experiencePerBlock.keySet());
        leaderboardBlocks.removeIf(material -> experiencePerBlock.get(material) < minXpThreshold);
    }

}
