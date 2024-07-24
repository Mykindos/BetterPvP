package me.mykindos.betterpvp.clans.clans.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.events.ClanEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Getter
public class ClanSetCoreLocationEvent extends ClanEvent<Clan> {

    private final boolean ignoreClaims;

    public ClanSetCoreLocationEvent(Player player, Clan clan, boolean ignoreClaims) {
        super(player, clan);
        this.ignoreClaims = ignoreClaims;
    }

}

