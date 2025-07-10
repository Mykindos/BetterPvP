package me.mykindos.betterpvp.game.achievements;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import me.mykindos.betterpvp.game.GamePlugin;
import me.mykindos.betterpvp.game.achievements.category.GameAchievementCategories;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
/**
 * Loads instances of {@link ChampionsWinAchievement} from a config, with the settings needed
 */
public class ChampionsWinAchievementLoader extends SingleSimpleAchievementConfigLoader<ChampionsWinAchievement> {
    @Inject
    public ChampionsWinAchievementLoader(GamePlugin plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return GameAchievementCategories.GAME;
    }

    @Override
    protected ChampionsWinAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new ChampionsWinAchievement(key, goal.intValue());
    }
}
