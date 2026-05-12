package me.mykindos.betterpvp.core.client.achievements.impl.dungeons.oakmist;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class OakmistAchievementLoader extends SingleSimpleAchievementConfigLoader<OakmistValleyAchievement> {

    @Inject
    public OakmistAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.DUNGEONS_OAKMIST_VALLEY_PERIOD;
    }

    @Override
    protected OakmistValleyAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new OakmistValleyAchievement(key, goal.intValue());
    }
}

