package me.mykindos.betterpvp.clans.clans.events;

import me.mykindos.betterpvp.clans.clans.Clan;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public class ChunkUnclaimEvent extends ClanTerritoryEvent {

    public ChunkUnclaimEvent(Player player, Clan targetClan) {
        super(player, targetClan, player.getChunk());
    }

    public ChunkUnclaimEvent(Player player, Clan targetClan, Chunk chunk) {
        super(player, targetClan, chunk);
    }
}
