package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;

@Getter
public class ClanPropertyUpdateEvent extends PropertyUpdateEvent {

    private final Clan clan;

    public ClanPropertyUpdateEvent(Clan clan, String property, Object value) {
        super(property, value);
        this.clan = clan;
    }

}
