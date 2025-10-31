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
public class PillageAchievement extends SingleSimpleAchievement {
    @Inject
    public PillageAchievement() {
        super("ATTACK!",
                new NamespacedKey("clans", "pillage_attack"),
                AchievementCategories.CLANS,
                AchievementType.GLOBAL,
                1d,
                ClientStat.CLANS_ATTACK_PILLAGE
        );
    }

    @Override
    public Material getMaterial(StatContainer container, String period) {
        return Material.TNT;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, String period) {
        return List.of("Pillage another Clan");
    }
}
