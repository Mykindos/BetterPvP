package me.mykindos.betterpvp.clans.clans.events;

import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.events.ClanEvent;
import org.bukkit.entity.Player;


public class ClanDominanceChangeEvent extends ClanEvent<IClan> {

    public ClanDominanceChangeEvent(Player player, IClan clan) {
        super(player, clan);
    }

}
