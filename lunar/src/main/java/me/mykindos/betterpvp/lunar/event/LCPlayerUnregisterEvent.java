package me.mykindos.betterpvp.lunar.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public final class LCPlayerUnregisterEvent extends PlayerEvent {

    @Getter private static HandlerList handlerList = new HandlerList();

    /**
     * Called whenever a player unregisters the LC plugin channel
     *
     * @param player The player who has unregistered from the plugin channel.
     */
    public LCPlayerUnregisterEvent(Player player) {
        super(player);
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

}