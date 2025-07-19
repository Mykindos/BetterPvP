package me.mykindos.betterpvp.core.client.achievements.impl.dungeons;

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
public class BraewoodCavernsAchievementLoader extends SingleSimpleAchievementConfigLoader<BraewoodCavernsPeriodAchievement> {
    @Inject
    public BraewoodCavernsAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.DUNGEONS_BRAEWOOD_CAVERNS_PERIOD;
    }

    @Override
    protected BraewoodCavernsPeriodAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new BraewoodCavernsPeriodAchievement(key, goal.intValue());
    }
}
