package me.mykindos.betterpvp.core.client.achievements.impl.events.bosses.deepcreature;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class DeepCreatureAchievementLoader extends SingleSimpleAchievementConfigLoader<DeepCreatureKillAchievement> {

    @Inject
    public DeepCreatureAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.EVENT_BOSS_DEEP_CREATURE;
    }

    @Override
    protected DeepCreatureKillAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new DeepCreatureKillAchievement(key, goal.intValue());
    }
}

