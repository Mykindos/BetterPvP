package me.mykindos.betterpvp.core.client.achievements.types.containertypes;

import me.mykindos.betterpvp.core.client.achievements.types.IAchievement;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.properties.PropertyContainer;

/**
 * Represents an achievement based off of {@link Gamer} data
 */
public interface IGamerAchievement extends IAchievement<Gamer, GamerPropertyUpdateEvent> {
    @Override
    default boolean isSameType(PropertyContainer container) {
        return container instanceof Gamer;
    }
}