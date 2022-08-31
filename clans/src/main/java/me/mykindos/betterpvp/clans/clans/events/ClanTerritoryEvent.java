package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.events.ClanEvent;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

@Getter
public class ClanTerritoryEvent extends ClanEvent<Clan> {

    private final Chunk chunk;

    public ClanTerritoryEvent(Player player, Clan clan, Chunk chunk) {
        super(player, clan, false);
        this.chunk = chunk;
    }
}
