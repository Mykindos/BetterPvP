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
 * Loads instances of {@link DemolisherAchievement} from config
 */
public class DemolisherAchievementLoader extends SingleSimpleAchievementConfigLoader<DemolisherAchievement> {

    @Inject
    public DemolisherAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.CLANS_CANNON_BLOCK_DAMAGE_TYPE;
    }

    @Override
    protected DemolisherAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new DemolisherAchievement(key, goal.intValue());
    }
}

