package me.mykindos.betterpvp.core.chat.events;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Getter
@Setter
public class ChatSentEvent extends CustomCancellableEvent {

    private final Player player;
    private IChatChannel channel;
    private Component message;
    private Component prefix;

    public ChatSentEvent(Player player, IChatChannel channel, Component prefix, Component message) {
        super(true);
        this.player = player;
        this.channel = channel;
        this.prefix = prefix;
        this.message = message;
    }

}
