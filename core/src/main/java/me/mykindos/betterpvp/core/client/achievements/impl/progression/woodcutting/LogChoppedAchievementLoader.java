package me.mykindos.betterpvp.core.client.achievements.impl.progression.woodcutting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class LogChoppedAchievementLoader extends SingleSimpleAchievementConfigLoader<LogChoppedAchievement> {

    @Inject
    public LogChoppedAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.PROGRESSION_WOODCUTTING;
    }

    @Override
    protected LogChoppedAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new LogChoppedAchievement(key, goal.intValue());
    }
}

