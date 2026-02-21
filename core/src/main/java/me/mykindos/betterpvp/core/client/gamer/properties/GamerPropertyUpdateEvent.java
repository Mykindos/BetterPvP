package me.mykindos.betterpvp.core.client.gamer.properties;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;

@Getter
public class GamerPropertyUpdateEvent extends PropertyUpdateEvent<Gamer> {

    public GamerPropertyUpdateEvent(Gamer container, String property, Object newValue, Object oldValue) {
        super(container, property, newValue, oldValue);
    }
}
