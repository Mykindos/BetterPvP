package me.mykindos.betterpvp.core.client.achievements.impl.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.NSingleGoalSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

@CustomLog
@Singleton
public class SetCoreAchievement extends NSingleGoalSimpleAchievement {
    @Inject
    public SetCoreAchievement() {
        super("Set Core", new NamespacedKey("champions", "set_core"), AchievementCategories.CLANS, AchievementType.GLOBAL, 1d, ClientStat.CLANS_SET_CORE);
    }

    @Override
    public Material getMaterial(StatContainer container, String period) {
        return Material.RESPAWN_ANCHOR;
    }
}
