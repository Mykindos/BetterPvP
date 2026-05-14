package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.skeletonking;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategory;
import me.mykindos.betterpvp.core.client.achievements.category.SubCategory;
import me.mykindos.betterpvp.core.client.achievements.category.types.EventCategory;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.events.BossStat;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

@Singleton
@SubCategory(EventCategory.class)
public class SkeletonKingKillCategory extends AchievementCategory {

    public SkeletonKingKillCategory() {
        super(AchievementCategories.EVENT_BOSS_SKELETON_KING);
    }

    @Override
    public ItemView getItemView() {
        return ItemView.builder()
                .material(Material.SKELETON_SKULL)
                .displayName(UtilMessage.deserialize("<white>Kill the Skeleton King"))
                .build();
    }

    @Override
    public ItemView getItemView(StatContainer container, StatFilterType type, @Nullable Period period) {
        final GenericStat skeletonKingStat = new GenericStat(BossStat.builder()
                .action(BossStat.Action.KILL)
                .bossName("Skeleton King")
                .build()
        );
        boolean killedSkeletonKing = skeletonKingStat.getStat(container, StatFilterType.ALL, null) >= 1;
        return ItemView.builder()
                .material(Material.SKELETON_SKULL)
                .displayName(UtilMessage.deserialize("<white>" + (killedSkeletonKing ? "Kill the Skeleton King" : "???")))
                .build();
    }
}
