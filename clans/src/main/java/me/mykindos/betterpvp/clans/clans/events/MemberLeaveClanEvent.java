package me.mykindos.betterpvp.clans.clans.events;

import me.mykindos.betterpvp.clans.clans.Clan;
import org.bukkit.entity.Player;

public class MemberLeaveClanEvent extends ClanEvent{
    public MemberLeaveClanEvent(Player player, Clan clan) {
        super(player, clan, true);
    }
}
