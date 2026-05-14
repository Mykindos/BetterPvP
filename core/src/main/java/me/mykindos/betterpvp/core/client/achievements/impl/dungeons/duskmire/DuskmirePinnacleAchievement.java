package me.mykindos.betterpvp.core.client.achievements.impl.dungeons.duskmire;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.impl.general.deaths.DeathAchievementLoader;
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
 * Super class, is either extended or loaded by a loader {@link DeathAchievementLoader}
 */
public class DuskmirePinnacleAchievement extends SingleSimpleAchievement {

    public DuskmirePinnacleAchievement(String key, int goal) {
        this(new NamespacedKey("dungeons", key), goal);
    }

    public DuskmirePinnacleAchievement(NamespacedKey key, int goal) {
        super("Beat the Duskmire Pinnacle", key,
                AchievementCategories.DUNGEONS_DUSKMIRE_PINNACLE_PERIOD,
                StatFilterType.SEASON,
                (long) goal,
                new GenericStat(
                        DungeonNativeStat.builder()
                                .action(DungeonNativeStat.Action.WIN)
                                .dungeonName("Duskmire Pinnacle")
                                .build()
                )
        );
    }

    @Override
    public String getName() {
        return "Beat the Duskmire Pinnacle " + getGoal().intValue();
    }

    @Override
    public Material getMaterial(StatContainer container, StatFilterType type, Period period) {
        return Material.MAGMA_BLOCK;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, StatFilterType type, Period period) {
        return List.of("<gray>Beat the Duskmire Pinnacle <yellow>" + getGoal().intValue() + "</yellow> times");
    }
}

