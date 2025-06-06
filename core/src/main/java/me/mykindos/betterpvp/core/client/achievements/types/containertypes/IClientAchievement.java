package me.mykindos.betterpvp.core.client.achievements.types.containertypes;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.achievements.IAchievement;
import me.mykindos.betterpvp.core.client.properties.ClientPropertyUpdateEvent;
import me.mykindos.betterpvp.core.properties.PropertyContainer;

/**
 * Represents an achievement based off of {@link Client} data
 */
public interface IClientAchievement extends IAchievement<Client, ClientPropertyUpdateEvent> {
    @Override
    default boolean isSameType(PropertyContainer container) {
        return container instanceof Client;
    }
}
