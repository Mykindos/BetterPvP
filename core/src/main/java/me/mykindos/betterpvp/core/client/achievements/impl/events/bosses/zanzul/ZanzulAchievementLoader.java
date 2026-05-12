package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.zanzul;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class ZanzulAchievementLoader extends SingleSimpleAchievementConfigLoader<ZanzulKillAchievement> {

    @Inject
    public ZanzulAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.EVENT_BOSS_ZANZUL;
    }

    @Override
    protected ZanzulKillAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new ZanzulKillAchievement(key, goal.intValue());
    }
}

