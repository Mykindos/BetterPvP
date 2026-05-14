package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.skeletonking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class SkeletonKingAchievementLoader extends SingleSimpleAchievementConfigLoader<SkeletonKingKillAchievement> {

    @Inject
    public SkeletonKingAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.EVENT_BOSS_SKELETON_KING;
    }

    @Override
    protected SkeletonKingKillAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new SkeletonKingKillAchievement(key, goal.intValue());
    }
}

