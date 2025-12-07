package me.mykindos.betterpvp.core.client.achievements.impl.champions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.champions.ChampionsSkillStat;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

@CustomLog
@Singleton
@BPvPListener
public class MagneticAxeAchievement extends SingleSimpleAchievement {
    @Inject
    public MagneticAxeAchievement() {
        super("It Bounces Back!",
                new NamespacedKey("champions", "magnetic_axe_100"),
                AchievementCategories.CHAMPIONS,
                AchievementType.GLOBAL,
                100L,
                new GenericStat(
                        ChampionsSkillStat.builder()
                        .action(ChampionsSkillStat.Action.USE)
                        .skillName("Magnetic Axe")
                        .build()
                )
        );
    }

    @Override
    public Material getMaterial(StatContainer container, String period) {
        return Material.IRON_AXE;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, String period) {
        return List.of("<gold>Use <white>Magnetic Axe <yellow>100 <gray>times");
    }
}
