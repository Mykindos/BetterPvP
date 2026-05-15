package me.mykindos.betterpvp.core.client.achievements.impl.progression.fishing;

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
 * Achievement for catching fish. Is either extended or loaded by {@link FishCaughtAchievementLoader}.
 */
public class FishCaughtAchievement extends SingleSimpleAchievement {

    public FishCaughtAchievement(String key, int goal) {
        this(new NamespacedKey("progression", key), goal);
    }

    public FishCaughtAchievement(NamespacedKey key, int goal) {
        super("Fish Caught", key,
                AchievementCategories.PROGRESSION_FISHING,
                StatFilterType.SEASON,
                (long) goal,
                ClientStat.FISH_CAUGHT
        );
    }

    @Override
    public String getName() {
        return "Catch " + getGoal().intValue() + " Fish";
    }

    @Override
    public Material getMaterial(StatContainer container, StatFilterType type, Period period) {
        return Material.COD;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, StatFilterType type, Period period) {
        return List.of("<gray>Catch <yellow>" + getGoal().intValue() + "</yellow> fish");
    }
}


