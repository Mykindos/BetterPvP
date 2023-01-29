package me.mykindos.betterpvp.clans.clans.events;

import me.mykindos.betterpvp.clans.clans.Clan;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;


public class ChunkClaimEvent extends ClanTerritoryEvent {

    public ChunkClaimEvent(Player player, Clan clan) {
        this(player, clan, player.getChunk());
    }

    public ChunkClaimEvent(Player player, Clan clan, Chunk chunk) {
        super(player, clan, chunk);
    }
}
