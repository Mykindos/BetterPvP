package me.mykindos.betterpvp.lunar.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;


public final class LCPlayerRegisterEvent extends PlayerEvent {

    @Getter private static HandlerList handlerList = new HandlerList();

    /**
     * Called whenever a player registers the LC plugin channel
     *
     * @param player The player registering as a Lunar Client user.
     */
    public LCPlayerRegisterEvent(Player player) {
        super(player);
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

}