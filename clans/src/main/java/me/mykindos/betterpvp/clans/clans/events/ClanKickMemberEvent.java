package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.client.Client;
import org.bukkit.entity.Player;

public class ClanKickMemberEvent extends ClanEvent{

    @Getter
    private final Client target;

    public ClanKickMemberEvent(Player player, Clan clan, Client target) {
        super(player, clan, true);
        this.target = target;
    }
}
