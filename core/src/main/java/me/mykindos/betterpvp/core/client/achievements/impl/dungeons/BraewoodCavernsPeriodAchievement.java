package me.mykindos.betterpvp.core.client.achievements.impl.dungeons;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.impl.general.deaths.DeathAchievementLoader;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.dungeons.DungeonStat;
import me.mykindos.betterpvp.core.utilities.model.NoReflection;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

@CustomLog
//Config loaded achievement, this class will be skipped by reflection
@NoReflection
/**
 * Super class, is either extended or loaded by a loader {@link DeathAchievementLoader}
 */
public class BraewoodCavernsPeriodAchievement extends SingleSimpleAchievement {

    public BraewoodCavernsPeriodAchievement(String key, int goal) {
        this(new NamespacedKey("dungeons", key), goal);
    }

    public BraewoodCavernsPeriodAchievement(NamespacedKey key, int goal) {
        super("Beat the Braewoods Cavern", key,
                AchievementCategories.DUNGEONS_BRAEWOOD_CAVERNS_PERIOD,
                AchievementType.PERIOD,
                (double) goal,
                DungeonStat.builder()
                        .action(DungeonStat.Action.WIN)
                        .dungeonName("Braewood Caverns")
                        .build()
        );
    }

    @Override
    public String getName() {
        return "Beat the Braewoods Cavern " + getGoal().intValue();
    }

    /**
     * gets the material for the itemprovider
     *
     * @param container
     * @param period
     * @return
     */
    @Override
    public Material getMaterial(StatContainer container, String period) {
        return Material.OAK_SAPLING;
    }

    /**
     * A helper method, to easily add a description to the lore
     * without duplicating adding the progress and completion component
     * Used in getLore
     *
     * @param container
     * @param period
     * @return
     */
    @Override
    public List<String> getStringDescription(StatContainer container, String period) {
        return List.of("<gray>Beat the Braewoods Cavern <yellow>" + getGoal().intValue() + "</yellow> times");
    }
}