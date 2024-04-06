package me.mykindos.betterpvp.clans.clans.events;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.events.ClanEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)

public class ClanCreateEvent extends ClanEvent<Clan> {

    public ClanCreateEvent(Player player, Clan clan) {
        super(player, clan);
    }

}

