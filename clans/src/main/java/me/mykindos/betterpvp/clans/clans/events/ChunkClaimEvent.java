package me.mykindos.betterpvp.clans.clans.events;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.events.ClanEvent;
import org.bukkit.entity.Player;

public class ChunkClaimEvent extends ClanEvent<Clan> {

    public ChunkClaimEvent(Player player, Clan clan) {
        super(player, clan, false);
    }
}
