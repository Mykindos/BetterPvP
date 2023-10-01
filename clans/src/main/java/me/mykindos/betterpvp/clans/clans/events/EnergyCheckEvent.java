package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.events.ClanEvent;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@Getter
public class EnergyCheckEvent extends ClanEvent<Clan> {
    public EnergyCheckEvent(Player player, Clan clan) {
        super(player, clan, false);
    }
}
