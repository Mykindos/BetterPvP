package me.mykindos.betterpvp.clans.achievements.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.achievements.category.ClansAchievementCategories;
import me.mykindos.betterpvp.core.client.achievements.AchievementType;
import me.mykindos.betterpvp.core.client.achievements.types.NSingleGoalSimpleAchievement;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import org.bukkit.NamespacedKey;

@CustomLog
@Singleton
public class SetCoreAchievement extends NSingleGoalSimpleAchievement {
    @Inject
    public SetCoreAchievement() {
        super("Set Core", new NamespacedKey("champions", "set_core"), ClansAchievementCategories.CLANS, AchievementType.GLOBAL, 1d, ClientStat.CLANS_SET_CORE);
    }

    /**
     * Gets the description of this achievement for the specified container
     * For use in UI's
     *
     * @param container the {@link PropertyContainer}
     * @return
     */
    @Override
    public Description getDescription(StatContainer container, String period) {
        return null;
    }
}
