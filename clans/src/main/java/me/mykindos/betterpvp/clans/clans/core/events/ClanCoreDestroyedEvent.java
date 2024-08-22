package me.mykindos.betterpvp.clans.clans.core.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClanCoreDestroyedEvent extends CustomEvent {

    private final Clan clan;

    public ClanCoreDestroyedEvent(Clan clan) {
        this.clan = clan;
    }
}
