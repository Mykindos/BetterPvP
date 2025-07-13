package me.mykindos.betterpvp.core.client.achievements.impl.game.ctf.flag;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.impl.game.wins.ChampionsWinAchievement;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
/**
 * Loads instances of {@link ChampionsWinAchievement} from a config, with the settings needed
 */
public class FlagCapturesAchievementLoader extends SingleSimpleAchievementConfigLoader<FlagCapturesAchievement> {
    @Inject
    public FlagCapturesAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.GAME_FLAG_CAPTURES;
    }

    @Override
    protected FlagCapturesAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new FlagCapturesAchievement(key, goal.intValue());
    }
}
