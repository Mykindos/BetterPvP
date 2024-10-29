package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a player changes territory
 */
@Getter
public class PlayerChangeTerritoryEvent extends CustomCancellableEvent {
    private final PlayerMoveEvent playerMoveEvent;
    private final Player player;
    private final Clan clan;
    private final Clan fromClan;
    private final Clan toClan;

    public PlayerChangeTerritoryEvent(PlayerMoveEvent playerMoveEvent, Player player, @Nullable Clan clan, @Nullable Clan fromClan, @Nullable Clan toClan) {
        super(true);
        this.playerMoveEvent = playerMoveEvent;
        this.player = player;
        this.clan = clan;
        this.fromClan = fromClan;
        this.toClan = toClan;
    }
}
