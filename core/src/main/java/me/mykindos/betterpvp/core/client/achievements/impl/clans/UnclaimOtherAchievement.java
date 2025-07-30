package me.mykindos.betterpvp.core.client.achievements.impl.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

@CustomLog
@Singleton
public class UnclaimOtherAchievement extends SingleSimpleAchievement {
    @Inject
    public UnclaimOtherAchievement() {
        super("Don't Get Greedy",
                new NamespacedKey("clans", "unclaim_other"),
                AchievementCategories.CLANS,
                AchievementType.GLOBAL,
                1d,
                ClientStat.CLANS_UNCLAIM_OTHER_TERRITORY
        );
    }

    @Override
    public Material getMaterial(StatContainer container, String period) {
        return Material.DIRT;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, String period) {
        return List.of( "Unclaim territory from another",
                        "Clan that does not have",
                        "enough members to hold",
                        "its territory");
    }
}
