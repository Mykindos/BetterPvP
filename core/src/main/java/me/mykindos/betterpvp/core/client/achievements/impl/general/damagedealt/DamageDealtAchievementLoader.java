package me.mykindos.betterpvp.core.client.achievements.impl.general.damagedealt;

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
 * Loads instances of {@link DamageDealtAchievement} from a config, with the settings needed
 */
public class DamageDealtAchievementLoader extends SingleSimpleAchievementConfigLoader<DamageDealtAchievement> {
    @Inject
    public DamageDealtAchievementLoader(Core core) {
        super(core);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.DAMAGE_DEALT_TYPE;
    }

    @Override
    protected DamageDealtAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new DamageDealtAchievement(key, goal);
    }
}

