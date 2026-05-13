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
/**
 * Loads instances of {@link CoreBreakerAchievement} from config
 */
public class CoreBreakerAchievementLoader extends SingleSimpleAchievementConfigLoader<CoreBreakerAchievement> {

    @Inject
    public CoreBreakerAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.CLANS_CORE_DAMAGE_TYPE;
    }

    @Override
    protected CoreBreakerAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new CoreBreakerAchievement(key, goal);
    }
}

