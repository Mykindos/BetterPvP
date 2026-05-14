package me.mykindos.betterpvp.core.client.achievements.impl.dungeons.duskmire;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class DuskmireAchievementLoader extends SingleSimpleAchievementConfigLoader<DuskmirePinnacleAchievement> {

    @Inject
    public DuskmireAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.DUNGEONS_DUSKMIRE_PINNACLE_PERIOD;
    }

    @Override
    protected DuskmirePinnacleAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new DuskmirePinnacleAchievement(key, goal.intValue());
    }
}

