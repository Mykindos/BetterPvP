package me.mykindos.betterpvp.core.client.achievements.impl.dungeons.global;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.dungeons.DungeonNativeStat;
import me.mykindos.betterpvp.core.server.Period;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

@CustomLog
@Singleton
public class BraewoodCitadelGlobalAchievement extends SingleSimpleAchievement {

    @Inject
    public BraewoodCitadelGlobalAchievement() {
        super("Beat the Braewood Citadel",
                new NamespacedKey("dungeons", "braewood_citadel_global_1"),
                AchievementCategories.DUNGEONS,
                StatFilterType.ALL,
                1L,
                new GenericStat(
                        DungeonNativeStat.builder()
                                .action(DungeonNativeStat.Action.WIN)
                                .dungeonName("Braewood Citadel")
                                .build()
                )
        );
    }

    @Override
    public Material getMaterial(StatContainer container, StatFilterType type, Period period) {
        return Material.CHISELED_STONE_BRICKS;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, StatFilterType type, Period period) {
        return List.of("Defeat the boss of the Braewood Citadel");
    }
}

