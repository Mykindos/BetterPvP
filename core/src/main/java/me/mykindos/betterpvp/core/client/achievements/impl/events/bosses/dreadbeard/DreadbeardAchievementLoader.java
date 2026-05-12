package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.dreadbeard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class DreadbeardAchievementLoader extends SingleSimpleAchievementConfigLoader<DreadbeardKillAchievement> {

    @Inject
    public DreadbeardAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.EVENT_BOSS_DREADBEARD;
    }

    @Override
    protected DreadbeardKillAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new DreadbeardKillAchievement(key, goal.intValue());
    }
}

