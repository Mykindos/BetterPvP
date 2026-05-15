package me.mykindos.betterpvp.core.client.achievements.impl.dungeons.braewood_citadel;

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
// Config loaded achievement, this class will be skipped by reflection
@NoReflection
/**
 * Super class, is either extended or loaded by a loader {@link BraewoodCitadelAchievementLoader}
 */
public class BraewoodCitadelPeriodAchievement extends SingleSimpleAchievement {

    public BraewoodCitadelPeriodAchievement(String key, int goal) {
        this(new NamespacedKey("dungeons", key), goal);
    }

    public BraewoodCitadelPeriodAchievement(NamespacedKey key, int goal) {
        super("Beat the Braewood Citadel", key,
                AchievementCategories.DUNGEONS_BRAEWOOD_CITADEL_PERIOD,
                StatFilterType.SEASON,
                (long) goal,
                new GenericStat(
                        DungeonNativeStat.builder()
                                .action(DungeonNativeStat.Action.WIN)
                                .dungeonName("Braewood Citadel")
                                .build()
                )
        );
    }

    @Override
    public String getName() {
        return "Beat the Braewood Citadel " + getGoal().intValue();
    }

    @Override
    public Material getMaterial(StatContainer container, StatFilterType type, Period period) {
        return Material.CHISELED_STONE_BRICKS;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, StatFilterType type, Period period) {
        return List.of("<gray>Beat the Braewood Citadel <yellow>" + getGoal().intValue() + "</yellow> times");
    }
}

