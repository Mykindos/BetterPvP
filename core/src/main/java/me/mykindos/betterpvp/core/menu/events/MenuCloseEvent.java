package me.mykindos.betterpvp.core.menu.events;

import lombok.Data;
import me.mykindos.betterpvp.core.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Data
public class MenuCloseEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final Menu menu;

    public MenuCloseEvent(Player player, Menu menu) {
        this.player = player;
        this.menu = menu;
    }


    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

}