package me.mykindos.betterpvp.core.framework;


import lombok.EqualsAndHashCode;
import lombok.Value;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@EqualsAndHashCode(callSuper = true)
@Value
public class ModuleLoadedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    String moduleName;

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
