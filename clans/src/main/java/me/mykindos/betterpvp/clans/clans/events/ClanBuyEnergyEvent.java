package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.events.ClanEvent;
import org.bukkit.entity.Player;

@Getter
public class ClanBuyEnergyEvent extends ClanEvent<Clan> {

    private final int amount;
    private final int cost;

    public ClanBuyEnergyEvent(Player player, Clan clan, int amount, int cost) {
        super(player, clan, false);
        this.amount = amount;
        this.cost = cost;
    }

}
