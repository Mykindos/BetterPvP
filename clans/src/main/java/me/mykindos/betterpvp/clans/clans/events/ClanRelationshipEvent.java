package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.events.ClanEvent;
import org.bukkit.entity.Player;

public class ClanRelationshipEvent extends ClanEvent<Clan> {

    @Getter
    private final Clan targetClan;

    public ClanRelationshipEvent(Player player, Clan clan, Clan targetClan) {
        super(player, clan, true);
        this.targetClan = targetClan;
    }
}
