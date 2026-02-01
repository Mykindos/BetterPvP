package me.mykindos.betterpvp.core.client.achievements.impl.game.wins;

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
 * Loads instances of {@link ChampionsWinAchievement} from a config, with the settings needed
 */
public class ChampionsWinAchievementLoader extends SingleSimpleAchievementConfigLoader<ChampionsWinAchievement> {
    @Inject
    public ChampionsWinAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.GAME_CHAMPIONS_WINS;
    }

    @Override
    protected ChampionsWinAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new ChampionsWinAchievement(key, goal.intValue());
    }
}
