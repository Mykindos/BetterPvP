package me.mykindos.betterpvp.clans.clans.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@Getter
@EqualsAndHashCode(callSuper = true)
public class ClanGainEnergyEvent extends CustomEvent {

    private final Clan clan;
    private final Player player;
    private final int energy;
    /** Human-readable label shown in energy gain notifications (e.g. "Killing Enemy", "Mining Ore"). */
    private final String reason;

    public ClanGainEnergyEvent(Clan clan, Player player, int energy, String reason) {
        this.clan = clan;
        this.player = player;
        this.energy = energy;
        this.reason = reason;
    }
}
