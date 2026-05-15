package me.mykindos.betterpvp.core.client.achievements.impl.progression.woodcutting;

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
 * Achievement for chopping logs. Is either extended or loaded by {@link LogChoppedAchievementLoader}.
 */
public class LogChoppedAchievement extends SingleSimpleAchievement {

    public LogChoppedAchievement(String key, int goal) {
        this(new NamespacedKey("progression", key), goal);
    }

    public LogChoppedAchievement(NamespacedKey key, int goal) {
        super("Logs Chopped", key,
                AchievementCategories.PROGRESSION_WOODCUTTING,
                StatFilterType.SEASON,
                (long) goal,
                ClientStat.LOG_CHOPPED
        );
    }

    @Override
    public String getName() {
        return "Chop " + getGoal().intValue() + " Logs";
    }

    @Override
    public Material getMaterial(StatContainer container, StatFilterType type, Period period) {
        return Material.OAK_LOG;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, StatFilterType type, Period period) {
        return List.of("<gray>Chop <yellow>" + getGoal().intValue() + "</yellow> logs");
    }
}


