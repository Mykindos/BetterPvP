package me.mykindos.betterpvp.core.client.achievements.impl.events.undeadchests;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.achievements.category.AchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.impl.general.deaths.DeathAchievementLoader;
import me.mykindos.betterpvp.core.client.achievements.types.SingleSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.server.Period;
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
public class OpenUndeadChests extends SingleSimpleAchievement {

    public OpenUndeadChests(String key, int goal) {
        this(new NamespacedKey("events", key), goal);
    }

    public OpenUndeadChests(NamespacedKey key, int goal) {
        super("Open Undead Chests", key,
                AchievementCategories.EVENT_UNDEAD_CHESTS,
                StatFilterType.ALL,
                (long) goal,
                new GenericStat(ClientStat.EVENT_UNDEAD_CITY_OPEN_CHEST)
        );
    }

    @Override
    public String getName() {
        return "Open " + getGoal().intValue() + " Undead Chests" ;
    }

    /**
     * gets the material for the itemprovider
     *
     * @param container
     * @param period
     * @return
     */
    @Override
    public Material getMaterial(StatContainer container, StatFilterType type, Period period) {
        return Material.WHITE_BANNER;
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
    public List<String> getStringDescription(StatContainer container, StatFilterType type, Period period) {
        return List.of("<gray>Open Undead Chests <yellow>" + getGoal().intValue() + "</yellow> times");
    }
}