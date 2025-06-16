package me.mykindos.betterpvp.core.client.achievements.types;

import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.types.loaded.ConfigLoadedAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks a single property when it is updated, completing at the goal
 * <p>Intermediate Constructors that are for {@link ConfigLoadedAchievement}
 * are expected to have a constructor that can be used with {@link SingleSimpleAchievementConfigLoader#instanstiateAchievement(NamespacedKey, Number)}</p>
 * @param <T> the container type
 * @param <E> the event type
 * @param <C> the {@link SingleSimpleAchievement#goal} type
 */
public abstract class SingleSimpleAchievement extends NSingleGoalSimpleAchievement {

    public SingleSimpleAchievement(NamespacedKey namespacedKey, NamespacedKey achievementCategory, AchievementType achievementType, Double goal, IStat watchedStat) {
        super(namespacedKey, achievementCategory, achievementType, goal, watchedStat);
    }

    protected IStat getKey() {
        return getWatchedStats().stream().findAny().orElseThrow();
    }

    protected Double getGoal() {
        return statGoals.get(getKey());
    }

    @SuppressWarnings("unchecked")
    protected Double getProperty(StatContainer container) {
        return getValue(container, getKey());
    }

    @Override
    protected List<Component> getProgressComponent(StatContainer container, @Nullable String period) {
        Double value = getValue(container, getKey(), period);
        List<Component> progressComponent = new ArrayList<>(super.getProgressComponent(container, period));
        Component bar = progressComponent.getFirst();
        progressComponent.removeFirst();
        progressComponent.addFirst(bar.append(UtilMessage.deserialize(" (<green>%s</green>/<yellow>%s</yellow>)", value, getGoal())));
        return progressComponent;
    }
}
