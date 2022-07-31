package me.mykindos.betterpvp.core.framework.scoreboard;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
public class ScoreboardUpdateEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();


    private final Player player;
    private boolean cancelled;

    @Override
    @NotNull
    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }


}
