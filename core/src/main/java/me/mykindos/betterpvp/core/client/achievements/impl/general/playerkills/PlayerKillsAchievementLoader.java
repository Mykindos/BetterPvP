package me.mykindos.betterpvp.core.client.achievements.impl.general.playerkills;

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
 * Loads instances of {@link PlayerKillsAchievement} from a config, with the settings needed
 */
public class PlayerKillsAchievementLoader extends SingleSimpleAchievementConfigLoader<PlayerKillsAchievement> {
    @Inject
    public PlayerKillsAchievementLoader(Core core) {
        super(core);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.PLAYER_KILLS_TYPE;
    }

    @Override
    protected PlayerKillsAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new PlayerKillsAchievement(key, goal.intValue());
    }
}

