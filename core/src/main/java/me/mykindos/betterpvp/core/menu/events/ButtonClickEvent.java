package me.mykindos.betterpvp.core.menu.events;


import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;


@EqualsAndHashCode(callSuper = true)
@Data
public class ButtonClickEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    private final Player player;
    private final Menu menu;
    private final Button button;
    private final ClickType clickType;
    private final Integer slot;

    private boolean cancelled;
    private String cancelReason;

    public void setCancelled(boolean cancelled, String reason){
        this.cancelled = cancelled;
        this.cancelReason = reason;
    }


}

