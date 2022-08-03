package me.mykindos.betterpvp.clans.clans.events;

import me.mykindos.betterpvp.clans.clans.Clan;
import org.bukkit.entity.Player;

public class ChunkClaimEvent extends ClanEvent {

    public ChunkClaimEvent(Player player, Clan clan) {
        super(player, clan, false);
    }
}
