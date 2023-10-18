package me.mykindos.betterpvp.progression.tree.mining.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.model.stats.ProgressionStatsRepository;
import me.mykindos.betterpvp.progression.tree.mining.Mining;
import me.mykindos.betterpvp.progression.tree.mining.MiningService;
import me.mykindos.betterpvp.progression.tree.mining.data.MiningData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Singleton
public class MiningRepository extends ProgressionStatsRepository<Mining, MiningData> {

    private final Map<Material, Long> experiencePerBlock = new EnumMap<>(Material.class);
    private final Set<Material> leaderboardBlocks = new HashSet<>();

    @Inject
    public MiningRepository(Progression progression, MiningService service) {
        super(progression, "Mining");
        loadConfig(progression.getConfig()); // Load before leaderboards
    }

    public Set<Material> getLeaderboardBlocks() {
        return Collections.unmodifiableSet(leaderboardBlocks);
    }

    public long getExperienceFor(Material material) {
        return experiencePerBlock.getOrDefault(material, 0L);
    }

    public String getDbMaterialsList() {
        return getLeaderboardBlocks().stream()
                .map(mat -> "'" + mat.name() + "'")
                .reduce((a, b) -> a + "," + b)
                .orElse("''");
    }

    @Override
    public CompletableFuture<MiningData> fetchDataAsync(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            final MiningData data = new MiningData();
            Statement statement = new Statement("CALL GetGamerOresMined(?, ?, ?)",
                    new UuidStatementValue(player),
                    new StringStatementValue(getDbMaterialsList()),
                    new StringStatementValue(plugin.getDatabasePrefix()));
            database.executeProcedure(statement, -1, result -> {
                try {
                    if (result.next()) {
                        data.setOresMined(result.getLong(1));
                    }
                } catch (SQLException e) {
                    log.error("Failed to load mining data for " + player, e);
                }
            });
            return data;
        }).exceptionally(throwable -> {
            log.error("Failed to load mining data for " + player, throwable);
            return null;
        });
    }

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        experiencePerBlock.clear();
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
        log.info("Loaded " + experiencePerBlock.size() + " mining blocks");

        final int minXpThreshold = config.getInt("mining.minLeaderboardBlockXp");
        leaderboardBlocks.clear();
        leaderboardBlocks.addAll(experiencePerBlock.keySet());
        leaderboardBlocks.removeIf(material -> experiencePerBlock.get(material) < minXpThreshold);
    }

    @Override
    protected Class<Mining> getTreeClass() {
        return Mining.class;
    }
}
