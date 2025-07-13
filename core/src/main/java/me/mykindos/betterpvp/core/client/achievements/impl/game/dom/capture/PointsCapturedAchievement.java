package me.mykindos.betterpvp.core.client.achievements.impl.game.dom.capture;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.impl.general.deaths.DeathAchievementLoader;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.game.DOMGameStat;
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
public class PointsCapturedAchievement extends SingleSimpleAchievement {

    public PointsCapturedAchievement(String key, int goal) {
        this(new NamespacedKey("game", key), goal);
    }

    public PointsCapturedAchievement(NamespacedKey key, int goal) {
        super("Flag Captures", key,
                AchievementCategories.GAME_POINTS_CAPTURED,
                AchievementType.GLOBAL,
                (double) goal,
                DOMGameStat.builder()
                        .action(DOMGameStat.Action.CONTROL_POINT_CAPTURED)
                        .build()
        );
    }

    @Override
    public String getName() {
        return "Points Captured " + getGoal().intValue();
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
        return Material.BEACON;
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
        return List.of("<gray>Capture Control Points <yellow>" + getGoal().intValue() + "</yellow> times");
    }
}