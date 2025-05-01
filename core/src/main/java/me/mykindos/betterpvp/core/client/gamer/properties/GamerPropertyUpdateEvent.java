package me.mykindos.betterpvp.core.client.gamer.properties;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;

@Getter
public class GamerPropertyUpdateEvent extends PropertyUpdateEvent<Gamer> {

    public GamerPropertyUpdateEvent(Gamer container, String property, Object value) {
        super(container, property, value);
    }
}
