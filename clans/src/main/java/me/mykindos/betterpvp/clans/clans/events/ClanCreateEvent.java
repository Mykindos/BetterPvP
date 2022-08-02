package me.mykindos.betterpvp.clans.clans.events;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.clans.Clan;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)

public class ClanCreateEvent extends ClanEvent {


    public ClanCreateEvent(Player player, Clan clan) {
        super(player, clan, true);
    }
}

