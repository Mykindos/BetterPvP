package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;

@Getter
public class ClanPropertyUpdateEvent extends PropertyUpdateEvent<Clan> {

    public ClanPropertyUpdateEvent(Clan container, String property, Object newValue, Object oldValue) {
        super(container, property, newValue, oldValue);
    }

}
