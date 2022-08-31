package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.events.ClanEvent;
import org.bukkit.entity.Player;

@Getter
public class MemberRankEvent extends ClanEvent<Clan> {

    private final ClanMember clanMember;

    public MemberRankEvent(Player player, Clan clan, ClanMember clanMember) {
        super(player, clan, false);
        this.clanMember = clanMember;
    }

}
