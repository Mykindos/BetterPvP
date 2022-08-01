package me.mykindos.betterpvp.core.chat.events;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;


/**
 * Useful for filtering out messages that have been ignored, or the target has chat disabled
 */
@Getter
@Setter
public class ChatReceivedEvent extends CustomEvent {

    private final Player player;
    private final Client client;
    private final Player target;
    private boolean cancelled;
    private String cancelReason;
    private Component message;
    private Component prefix;

    public ChatReceivedEvent(Player player, Client client, Player target, Component prefix, Component message) {
        super(true);
        this.player = player;
        this.client = client;
        this.target = target;
        this.prefix = prefix;
        this.message = message;
        this.cancelReason = "";
    }

}