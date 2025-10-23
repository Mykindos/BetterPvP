package me.mykindos.betterpvp.core.client.achievements.impl.dungeons.global;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.dungeons.DungeonNativeStat;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

@CustomLog
@Singleton
public class BraewoodCavernsAchievement extends SingleSimpleAchievement {
    @Inject
    public BraewoodCavernsAchievement() {
        super("Beat the Braewood Caverns",
                new NamespacedKey("dungeons", "braewood_caverns_global_1"),
                AchievementCategories.DUNGEONS,
                AchievementType.GLOBAL,
                1d,
                DungeonNativeStat.builder()
                        .action(DungeonNativeStat.Action.WIN)
                        .dungeonName("Braewood Caverns")
                        .build()
        );
    }

    @Override
    public Material getMaterial(StatContainer container, String period) {
        return Material.OAK_WOOD;
    }

    @Override
    public List<String> getStringDescription(StatContainer container, String period) {
        return List.of("Defeat the boss of the Braewood Caverns");
    }
}
