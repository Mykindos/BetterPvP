package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import org.bukkit.entity.Player;

@Getter
public class ClanKickMemberEvent extends MemberRankEvent {

    public ClanKickMemberEvent(Player player, Clan clan, ClanMember target) {
        super(player, clan, target);
    }
}
