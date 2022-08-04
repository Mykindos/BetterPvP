package me.mykindos.betterpvp.core.framework.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class CustomEvent extends Event{

    private static final HandlerList handlers = new HandlerList();

    public CustomEvent(boolean async){
        super(async);
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
