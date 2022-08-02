package me.mykindos.betterpvp.clans.clans.events;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.clans.Clan;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
public class ClanDisbandEvent extends ClanEvent {

    public ClanDisbandEvent(Player player, Clan clan) {
        super(player, clan, true);
    }
}