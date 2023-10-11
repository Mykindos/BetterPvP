package me.mykindos.betterpvp.progression.tree.mining;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.Progression;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

@Slf4j
@Singleton
public class MiningService implements ConfigAccessor {

    private final Map<Material, Long> experiencePerBlock = new EnumMap<>(Material.class);
    private final Set<Material> leaderboardBlocks = new HashSet<>();

    @Inject
    public MiningService(Progression progression) {
        loadConfig(progression.getConfig()); // Load before leaderboards
    }

    public long getExperience(Material material) {
        return experiencePerBlock.getOrDefault(material, 0L);
    }

    public Set<Material> getLeaderboardBlocks() {
        return Collections.unmodifiableSet(leaderboardBlocks);
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

}
