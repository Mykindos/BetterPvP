package me.mykindos.betterpvp.clans.clans.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ClanCreateEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    @Override
    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    private Player player;
    private String clanName;

    public ClanCreateEvent(Player player, String clanName) {
        this.player = player;
        this.clanName = clanName;
    }

    public String getClanName() {
        return clanName;
    }

    public Player getPlayer() {
        return player;
    }


    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }

}

