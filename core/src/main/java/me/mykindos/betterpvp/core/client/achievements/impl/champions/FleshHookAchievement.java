package me.mykindos.betterpvp.core.client.achievements.impl.champions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.champions.ChampionsSkillStat;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

@CustomLog
@Singleton
public class FleshHookAchievement extends SingleSimpleAchievement {
    @Inject
    public FleshHookAchievement() {
        super("Come Back Here!",
                new NamespacedKey("champions", "flesh_hook_100"),
                AchievementCategories.CHAMPIONS,
                AchievementType.GLOBAL,
                100d,
                ChampionsSkillStat.builder()
                        .action(ChampionsSkillStat.Action.USE)
                        .skillName("Flesh Hook")
                        .build()
        );
    }

    @Override
    public Material getMaterial(StatContainer container, String period) {
        return Material.IRON_AXE;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, String period) {
        return List.of("<gold>Use <white>Flesh Hook <yellow>100 <gray>times");
    }
}
