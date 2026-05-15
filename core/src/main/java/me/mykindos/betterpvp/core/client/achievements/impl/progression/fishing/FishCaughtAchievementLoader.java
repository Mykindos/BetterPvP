package me.mykindos.betterpvp.core.client.achievements.impl.progression.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class FishCaughtAchievementLoader extends SingleSimpleAchievementConfigLoader<FishCaughtAchievement> {

    @Inject
    public FishCaughtAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.PROGRESSION_FISHING;
    }

    @Override
    protected FishCaughtAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new FishCaughtAchievement(key, goal.intValue());
    }
}

