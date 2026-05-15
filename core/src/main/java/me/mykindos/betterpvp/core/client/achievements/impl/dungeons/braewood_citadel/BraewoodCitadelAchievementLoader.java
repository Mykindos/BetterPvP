package me.mykindos.betterpvp.core.client.achievements.impl.dungeons.braewood_citadel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class BraewoodCitadelAchievementLoader extends SingleSimpleAchievementConfigLoader<BraewoodCitadelPeriodAchievement> {

    @Inject
    public BraewoodCitadelAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.DUNGEONS_BRAEWOOD_CITADEL_PERIOD;
    }

    @Override
    protected BraewoodCitadelPeriodAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new BraewoodCitadelPeriodAchievement(key, goal.intValue());
    }
}

