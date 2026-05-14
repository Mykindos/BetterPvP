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
public class SoulKnightGlobalAchievement extends SingleSimpleAchievement {

    @Inject
    public SoulKnightGlobalAchievement() {
        super("Kill the Soul Knight",
                new NamespacedKey("events", "soul_knight_global_1"),
                AchievementCategories.EVENT,
                StatFilterType.ALL,
                1L,
                new GenericStat(
                        BossStat.builder()
                                .action(BossStat.Action.KILL)
                                .bossName("Soul Knight")
                                .build()
                )
        );
    }

    @Override
    public Material getMaterial(StatContainer container, StatFilterType type, Period period) {
        return Material.SOUL_LANTERN;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, StatFilterType type, Period period) {
        return List.of("Kill the Soul Knight");
    }
}

