package me.mykindos.betterpvp.champions.properties;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;

//todo remove, does not look like it is being used
@Getter
public class ChampionsPropertyUpdateEvent extends PropertyUpdateEvent<Gamer> {

    public ChampionsPropertyUpdateEvent(Gamer container, String property, Object value) {
        super(container, property, value);
    }
}
