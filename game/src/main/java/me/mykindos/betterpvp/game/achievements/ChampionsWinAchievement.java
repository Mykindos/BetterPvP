package me.mykindos.betterpvp.game.achievements;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.impl.gamer.deaths.DeathAchievementLoader;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameMapStat;
import me.mykindos.betterpvp.core.utilities.model.NoReflection;
import me.mykindos.betterpvp.game.achievements.category.GameAchievementCategories;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;

@CustomLog
//Config loaded achievement, this class will be skipped by reflaction
@NoReflection
/**
 * Super class, is either extended or loaded by a loader {@link DeathAchievementLoader}
 */
public class ChampionsWinAchievement extends SingleSimpleAchievement {

    public ChampionsWinAchievement(String key, int goal) {
        this(new NamespacedKey("game", key), goal);
    }

    public ChampionsWinAchievement(NamespacedKey key, int goal) {
        super("Champions Wins", key,
                GameAchievementCategories.GAME,
                AchievementType.GLOBAL,
                (double) goal,
                GameMapStat.builder()
                        .action(GameMapStat.Action.WIN)
                        .build()
        );
    }

    @Override
    public String getName() {
        return "Champions Wins " + getGoal().intValue();
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
        return Material.BELL;
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
        return List.of("<gray>Win Champions Games <yellow>" + getGoal().intValue() + "</yellow> times");
    }
}