package me.mykindos.betterpvp.core.client.events;


import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.core.client.Client;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@EqualsAndHashCode(callSuper = true)
@Value
public class ClientLoginEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    Client client;
    Player player;

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
