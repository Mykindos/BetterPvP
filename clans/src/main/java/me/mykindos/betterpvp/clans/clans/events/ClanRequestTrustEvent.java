package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;


@Getter
public class ClanRequestTrustEvent extends CustomCancellableEvent {

    private final Player player;
    private final Clan clan;
    private final Clan targetClan;

    public ClanRequestTrustEvent(Player player, Clan clan, Clan targetClan) {
        this.player = player;
        this.clan = clan;
        this.targetClan = targetClan;
    }
}
