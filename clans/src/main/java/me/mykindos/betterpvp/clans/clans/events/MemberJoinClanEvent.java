package me.mykindos.betterpvp.clans.clans.events;

import me.mykindos.betterpvp.clans.clans.Clan;
import org.bukkit.entity.Player;

public class MemberJoinClanEvent extends ClanEvent {
    public MemberJoinClanEvent(Player player, Clan clan) {
        super(player, clan, true);
    }
}
