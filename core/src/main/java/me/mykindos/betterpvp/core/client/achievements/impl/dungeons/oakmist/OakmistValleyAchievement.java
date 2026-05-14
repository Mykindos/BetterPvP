package me.mykindos.betterpvp.core.client.achievements.impl.dungeons.oakmist;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.dungeons.DungeonNativeStat;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.model.NoReflection;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

@CustomLog
@NoReflection
/**
 * Super class, is either extended or loaded by a loader {@link OakmistAchievementLoader}
 */
public class OakmistValleyAchievement extends SingleSimpleAchievement {

    public OakmistValleyAchievement(String key, int goal) {
        this(new NamespacedKey("dungeons", key), goal);
    }

    public OakmistValleyAchievement(NamespacedKey key, int goal) {
        super("Beat the Oakmist Valley", key,
                AchievementCategories.DUNGEONS_OAKMIST_VALLEY_PERIOD,
                StatFilterType.SEASON,
                (long) goal,
                new GenericStat(
                        DungeonNativeStat.builder()
                                .action(DungeonNativeStat.Action.WIN)
                                .dungeonName("Oakmist Valley")
                                .build()
                )
        );
    }

    @Override
    public String getName() {
        return "Beat the Oakmist Valley " + getGoal().intValue();
    }

    @Override
    public Material getMaterial(StatContainer container, StatFilterType type, Period period) {
        return Material.OAK_LEAVES;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, StatFilterType type, Period period) {
        return List.of("<gray>Beat the Oakmist Valley <yellow>" + getGoal().intValue() + "</yellow> times");
    }
}

