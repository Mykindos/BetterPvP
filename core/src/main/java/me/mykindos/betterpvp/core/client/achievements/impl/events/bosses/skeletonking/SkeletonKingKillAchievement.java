package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.skeletonking;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.events.BossStat;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.model.NoReflection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

@CustomLog
@NoReflection
public class SkeletonKingKillAchievement extends SingleSimpleAchievement {

    public SkeletonKingKillAchievement(String key, int goal) {
        this(new NamespacedKey("events", key), goal);
    }

    public SkeletonKingKillAchievement(NamespacedKey key, int goal) {
        super("Kill the Skeleton King", key,
                AchievementCategories.EVENT_BOSS_SKELETON_KING,
                StatFilterType.SEASON,
                (long) goal,
                new GenericStat(
                        BossStat.builder()
                                .action(BossStat.Action.KILL)
                                .bossName("Skeleton King")
                                .build()
                )
        );
    }

    @Override
    public String getName() {
        return "Kill the Skeleton King " + getGoal().intValue() + " times";
    }

    @Override
    public Component getDisplayName(StatContainer container, StatFilterType type, Period period) {
        final GenericStat skeletonKingStat = new GenericStat(BossStat.builder()
                .action(BossStat.Action.KILL)
                .bossName("Skeleton King")
                .build()
        );
        boolean killedSkeletonKing = skeletonKingStat.getStat(container, StatFilterType.ALL, null) >= 1;
        return Component.text(killedSkeletonKing ? getName() : "???", NamedTextColor.WHITE);
    }

    @Override
    public Material getMaterial(StatContainer container, StatFilterType type, Period period) {
        return Material.SKELETON_SKULL;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, StatFilterType type, Period period) {
        final GenericStat skeletonKingStat = new GenericStat(BossStat.builder()
                .action(BossStat.Action.KILL)
                .bossName("Skeleton King")
                .build()
        );
        boolean killedSkeletonKing = skeletonKingStat.getStat(container, StatFilterType.ALL, null) >= 1;
        if (!killedSkeletonKing) {
            return List.of("<gray>Kill the <white>???</white> <yellow>" + getGoal().intValue() + "</yellow> times");
        }
        return List.of("<gray>Kill the <white>Skeleton King</white> <yellow>" + getGoal().intValue() + "</yellow> times");
    }
}

