package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@Getter
public class ClanChangeTerritoryEvent extends CustomEvent {
    private final Player player;
    private final Clan fromClan;
    private final Clan toClan;
    public ClanChangeTerritoryEvent(Player player, Clan fromClan, Clan toClan) {
        this.player = player;
        this.fromClan = fromClan;
        this.toClan = toClan;
    }
}
