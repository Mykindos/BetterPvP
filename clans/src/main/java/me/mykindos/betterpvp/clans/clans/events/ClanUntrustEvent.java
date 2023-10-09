package me.mykindos.betterpvp.clans.clans.events;

import me.mykindos.betterpvp.clans.clans.Clan;
import org.bukkit.entity.Player;

public class ClanUntrustEvent extends ClanRelationshipEvent {
    public ClanUntrustEvent(Player player, Clan clan, Clan targetClan) {
        super(player, clan, targetClan);
    }
}
