package me.mykindos.betterpvp.clans.clans.events;

import me.mykindos.betterpvp.clans.clans.Clan;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public class ChunkClaimEvent extends ClanEvent {

    private final Chunk chunk;
    public ChunkClaimEvent(Player player, Clan clan, Chunk chunk) {
        super(player, clan, false);
        this.chunk = chunk;
    }
}
