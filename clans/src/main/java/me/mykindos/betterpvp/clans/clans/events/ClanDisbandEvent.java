package me.mykindos.betterpvp.clans.clans.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.clans.Clan;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


@EqualsAndHashCode(callSuper = true)
@Data
public class ClanDisbandEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final Clan clan;
    private boolean cancelled;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }

    @Override
    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}