package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import org.bukkit.entity.Player;


public class ClanInviteMemberEvent extends ClanEvent {

    @Getter
    private final Player target;

    public ClanInviteMemberEvent(Player player, Clan clan, Player target) {
        super(player, clan, false);
        this.target = target;
    }
}
