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
 * Loads instances of {@link DominatorAchievement} from config
 */
public class DominatorAchievementLoader extends SingleSimpleAchievementConfigLoader<DominatorAchievement> {

    @Inject
    public DominatorAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.CLANS_DOMINANCE_GAINED_TYPE;
    }

    @Override
    protected DominatorAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new DominatorAchievement(key, goal);
    }
}

