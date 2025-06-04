package me.mykindos.betterpvp.clans.achievements.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.achievements.category.ClansAchievementCategories;
import me.mykindos.betterpvp.clans.achievements.stats.ClansClientProperties;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.types.NSingleGoalSimpleAchievement;
import me.mykindos.betterpvp.core.client.achievements.types.containertypes.IClientAchievement;
import me.mykindos.betterpvp.core.client.properties.ClientPropertyUpdateEvent;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import org.bukkit.NamespacedKey;

@CustomLog
@Singleton
public class SetCoreAchievement extends NSingleGoalSimpleAchievement<Client, ClientPropertyUpdateEvent> implements IClientAchievement {
    @Inject
    public SetCoreAchievement() {
        super(new NamespacedKey("champions", "set_core"), ClansAchievementCategories.CLANS, 1, ClansClientProperties.SET_CORE);
    }

    /**
     * Gets the description of this achievement for the specified container
     * For use in UI's
     *
     * @param container the {@link PropertyContainer}
     * @return
     */
    @Override
    public Description getDescription(Client container) {
        return null;
    }
}
