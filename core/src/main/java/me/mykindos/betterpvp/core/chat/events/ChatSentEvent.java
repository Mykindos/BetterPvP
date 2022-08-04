package me.mykindos.betterpvp.core.chat.events;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Collection;

@Getter
@Setter
public class ChatSentEvent extends CustomCancellableEvent {

    private final Player player;
    private Collection<? extends Player> targets;
    private Component message;
    private Component prefix;

    private boolean cancelled;
    private String cancelReason;

    public ChatSentEvent(Player player, Collection<? extends Player> targets, Component prefix, Component message) {
        super(true);
        this.player = player;
        this.targets = targets;
        this.prefix = prefix;
        this.message = message;
        this.cancelReason = "";
    }

}
