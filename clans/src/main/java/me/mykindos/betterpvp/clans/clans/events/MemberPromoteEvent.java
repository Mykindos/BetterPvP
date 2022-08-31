package me.mykindos.betterpvp.clans.clans.events;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import org.bukkit.entity.Player;

public class MemberPromoteEvent extends MemberRankEvent {

    public MemberPromoteEvent(Player player, Clan clan, ClanMember clanMember) {
        super(player, clan, clanMember);
    }

}
