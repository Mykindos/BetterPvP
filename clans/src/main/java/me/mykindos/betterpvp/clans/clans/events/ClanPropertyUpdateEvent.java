package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;

@Getter
public class ClanPropertyUpdateEvent extends PropertyUpdateEvent {

    private final Clan clan;

    public ClanPropertyUpdateEvent(Clan clan, String property, Object value) {
        this(clan, property, value, false);
    }

    public ClanPropertyUpdateEvent(Clan clan, String property, Object value, boolean updateScoreboard) {
        super(property, value, updateScoreboard);
        this.clan = clan;
    }

}
