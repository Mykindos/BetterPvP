package me.mykindos.betterpvp.core.client.achievements.impl.general.deaths;

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
 * Loads instances of {@link DeathAchievement} from a config, with the settings needed
 */
public class DeathAchievementLoader extends SingleSimpleAchievementConfigLoader<DeathAchievement> {
    @Inject
    public DeathAchievementLoader(Core core) {
        super(core);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.DEATH_TYPE;
    }

    @Override
    protected DeathAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new DeathAchievement(key, goal.intValue());
    }
}
