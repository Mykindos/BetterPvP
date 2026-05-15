package me.mykindos.betterpvp.core.client.achievements.impl.progression.mining;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.model.NoReflection;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

@CustomLog
// Config-loaded achievement, this class will be skipped by reflection
@NoReflection
/**
 * Achievement for mining ores. Is either extended or loaded by {@link OreMinedAchievementLoader}.
 */
public class OreMinedAchievement extends SingleSimpleAchievement {

    public OreMinedAchievement(String key, int goal) {
        this(new NamespacedKey("progression", key), goal);
    }

    public OreMinedAchievement(NamespacedKey key, int goal) {
        super("Ores Mined", key,
                AchievementCategories.PROGRESSION_MINING,
                StatFilterType.SEASON,
                (long) goal,
                ClientStat.ORE_MINED
        );
    }

    @Override
    public String getName() {
        return "Mine " + getGoal().intValue() + " Ores";
    }

    @Override
    public Material getMaterial(StatContainer container, StatFilterType type, Period period) {
        return Material.IRON_ORE;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, StatFilterType type, Period period) {
        return List.of("<gray>Mine <yellow>" + getGoal().intValue() + "</yellow> ores");
    }
}

