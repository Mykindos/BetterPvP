package me.mykindos.betterpvp.clans.clans.events;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.events.ClanEvent;
import org.bukkit.entity.Player;

public class ChunkUnclaimEvent extends ClanEvent<Clan> {

    public ChunkUnclaimEvent(Player player, Clan targetClan) {
        super(player, targetClan, false);
    }
}
