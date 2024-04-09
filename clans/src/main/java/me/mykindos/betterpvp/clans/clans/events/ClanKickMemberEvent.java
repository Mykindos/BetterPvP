package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.components.clans.events.ClanEvent;
import org.bukkit.entity.Player;

@Getter
public class ClanKickMemberEvent extends ClanEvent<Clan> {

    private final Client target;

    public ClanKickMemberEvent(Player player, Clan clan, Client target) {
        super(player, clan);
        this.target = target;
    }
}
