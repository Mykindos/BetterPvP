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
public class DestroyCoreAchievement extends SingleSimpleAchievement {
    @Inject
    public DestroyCoreAchievement() {
        super("A Short Victorious War",
                new NamespacedKey("clans", "core_destroy"),
                AchievementCategories.CLANS,
                AchievementType.GLOBAL,
                1d,
                ClientStat.CLANS_DESTROY_CORE
        );
    }

    @Override
    public Material getMaterial(StatContainer container, String period) {
        return Material.BEACON;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, String period) {
        return List.of("Destroy a pillaged Clan's core");
    }
}
