package me.mykindos.betterpvp.core.framework.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class CustomEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    private String cancelReason;

    public CustomEvent(boolean async){
        super(async);
    }

    public void cancel(String reason) {
        this.cancelled = true;
        this.cancelReason = reason;
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
