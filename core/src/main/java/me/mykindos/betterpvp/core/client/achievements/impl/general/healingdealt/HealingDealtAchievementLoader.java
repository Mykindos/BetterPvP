package me.mykindos.betterpvp.core.client.achievements.impl.general.healingdealt;

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
 * Loads instances of {@link HealingDealtAchievement} from a config, with the settings needed
 */
public class HealingDealtAchievementLoader extends SingleSimpleAchievementConfigLoader<HealingDealtAchievement> {
    @Inject
    public HealingDealtAchievementLoader(Core core) {
        super(core);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.HEALING_DEALT_TYPE;
    }

    @Override
    protected HealingDealtAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new HealingDealtAchievement(key, goal);
    }
}

