package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.soulknight;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class SoulKnightAchievementLoader extends SingleSimpleAchievementConfigLoader<SoulKnightKillAchievement> {

    @Inject
    public SoulKnightAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.EVENT_BOSS_SOUL_KNIGHT;
    }

    @Override
    protected SoulKnightKillAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new SoulKnightKillAchievement(key, goal.intValue());
    }
}

