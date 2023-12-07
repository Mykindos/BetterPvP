package me.mykindos.betterpvp.champions.properties;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;


@Getter
public class ChampionsPropertyUpdateEvent extends PropertyUpdateEvent {

    private final Gamer gamer;

    public ChampionsPropertyUpdateEvent(Gamer gamer, String property, Object value) {
        super(property, value);
        this.gamer = gamer;
    }
}
