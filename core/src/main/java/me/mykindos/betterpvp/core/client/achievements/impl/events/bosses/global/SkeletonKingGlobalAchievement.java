package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.global;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.events.BossStat;
import me.mykindos.betterpvp.core.server.Period;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

@CustomLog
@Singleton
public class SkeletonKingGlobalAchievement extends SingleSimpleAchievement {

    @Inject
    public SkeletonKingGlobalAchievement() {
        super("Kill the Skeleton King",
                new NamespacedKey("events", "skeleton_king_global_1"),
                AchievementCategories.EVENT,
                StatFilterType.ALL,
                1L,
                new GenericStat(
                        BossStat.builder()
                                .action(BossStat.Action.KILL)
                                .bossName("Skeleton King")
                                .build()
                )
        );
    }

    @Override
    public Material getMaterial(StatContainer container, StatFilterType type, Period period) {
        return Material.WITHER_SKELETON_SKULL;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, StatFilterType type, Period period) {
        return List.of("Kill the Skeleton King");
    }
}

