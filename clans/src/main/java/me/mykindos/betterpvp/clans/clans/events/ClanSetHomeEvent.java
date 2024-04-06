package me.mykindos.betterpvp.clans.clans.events;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.events.ClanEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)

public class ClanSetHomeEvent extends ClanEvent<Clan> {

    public ClanSetHomeEvent(Player player, Clan clan) {
        super(player, clan);
    }

}

