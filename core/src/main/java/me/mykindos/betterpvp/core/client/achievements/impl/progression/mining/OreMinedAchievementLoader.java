package me.mykindos.betterpvp.core.client.achievements.impl.progression.mining;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievementConfigLoader;
import org.bukkit.NamespacedKey;

@Singleton
@CustomLog
public class OreMinedAchievementLoader extends SingleSimpleAchievementConfigLoader<OreMinedAchievement> {

    @Inject
    public OreMinedAchievementLoader(Core plugin) {
        super(plugin);
    }

    @Override
    public NamespacedKey getTypeKey() {
        return AchievementCategories.PROGRESSION_MINING;
    }

    @Override
    protected OreMinedAchievement instanstiateAchievement(NamespacedKey key, Double goal) {
        return new OreMinedAchievement(key, goal.intValue());
    }
}

