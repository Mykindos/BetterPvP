package me.mykindos.betterpvp.core.client.achievements.impl.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class UpkeeperAchievementLoader extends SingleSimpleAchievementConfigLoader<UpkeeperAchievement> {

    @Inject
    public UpkeeperAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.CLANS_ENERGY_COLLECTED_TYPE;
    }

    @Override
    protected UpkeeperAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new UpkeeperAchievement(key, goal.intValue());
    }
}

