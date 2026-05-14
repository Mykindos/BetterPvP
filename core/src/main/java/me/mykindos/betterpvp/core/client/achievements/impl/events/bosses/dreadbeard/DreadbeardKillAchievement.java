package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.dreadbeard;

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
public class DreadbeardKillAchievement extends SingleSimpleAchievement {

    public DreadbeardKillAchievement(String key, int goal) {
        this(new NamespacedKey("events", key), goal);
    }

    public DreadbeardKillAchievement(NamespacedKey key, int goal) {
        super("Kill Dreadbeard", key,
                AchievementCategories.EVENT_BOSS_DREADBEARD,
                StatFilterType.SEASON,
                (long) goal,
                new GenericStat(
                        BossStat.builder()
                                .action(BossStat.Action.KILL)
                                .bossName("Dreadbeard")
                                .build()
                )
        );
    }

    @Override
    public String getName() {
        return "Kill Dreadbeard " + getGoal().intValue() + " times";
    }

    @Override
    public Material getMaterial(StatContainer container, StatFilterType type, Period period) {
        return Material.NAUTILUS_SHELL;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, StatFilterType type, Period period) {
        return List.of("<gray>Kill <white>Dreadbeard</white> <yellow>" + getGoal().intValue() + "</yellow> times");
    }
}

