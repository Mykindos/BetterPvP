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
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.server.Period;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    public Component getDisplayName(StatContainer container, StatFilterType type, Period period) {
        final GenericStat skeletonKingStat = new GenericStat(BossStat.builder()
                .action(BossStat.Action.KILL)
                .bossName("Skeleton King")
                .build()
        );
        boolean killedSkeletonKing = skeletonKingStat.getStat(container, StatFilterType.ALL, null) >= 1;
        return killedSkeletonKing
                ? Translations.component(translationBase() + ".name").color(NamedTextColor.WHITE)
                : Component.text("???", NamedTextColor.WHITE);
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
            return List.of("<gray>???");
        }
        return List.of("Kill the Skeleton King");
    }
}

