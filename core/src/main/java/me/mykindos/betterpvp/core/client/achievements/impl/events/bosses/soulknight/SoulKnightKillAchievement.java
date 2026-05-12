package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.soulknight;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.events.BossStat;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.model.NoReflection;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

@CustomLog
@NoReflection
public class SoulKnightKillAchievement extends SingleSimpleAchievement {

    public SoulKnightKillAchievement(String key, int goal) {
        this(new NamespacedKey("events", key), goal);
    }

    public SoulKnightKillAchievement(NamespacedKey key, int goal) {
        super("Kill the Soul Knight", key,
                AchievementCategories.EVENT_BOSS_SOUL_KNIGHT,
                StatFilterType.SEASON,
                (long) goal,
                new GenericStat(
                        BossStat.builder()
                                .action(BossStat.Action.KILL)
                                .bossName("Soul Knight")
                                .build()
                )
        );
    }

    @Override
    public String getName() {
        return "Kill the Soul Knight " + getGoal().intValue() + " times";
    }

    @Override
    public Material getMaterial(StatContainer container, StatFilterType type, Period period) {
        return Material.SOUL_LANTERN;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, StatFilterType type, Period period) {
        return List.of("<gray>Kill the <white>Soul Knight</white> <yellow>" + getGoal().intValue() + "</yellow> times");
    }
}

