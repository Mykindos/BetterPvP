package me.mykindos.betterpvp.core.chat.events;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@Getter
@Setter
public class ChatSentEvent extends CustomCancellableEvent {

    private final Player player;
    private IChatChannel channel;
    private Component message;
    private Component prefix;
    /**
     * Optional per-recipient message renderer. When set, the chat pipeline builds the delivered message
     * for each recipient by calling this with that recipient (instead of broadcasting the shared
     * {@link #message}). This allows viewer-specific rendering — e.g. localizing an item hover into each
     * recipient's locale — while keeping recipient selection, mute/ignore checks, routing, formatting and
     * logging inside the chat system. The renderer's output is server-controlled and is not run through the
     * chat filter, so callers must ensure it contains no unsanitized user input. {@link #message} is still
     * used for filtering/logging and as the fallback for recipients when no renderer is set.
     */
    private @Nullable Function<Player, Component> messageRenderer;

    public ChatSentEvent(Player player, IChatChannel channel, Component prefix, Component message) {
        this(player, channel, prefix, message, null);
    }

    public ChatSentEvent(Player player, IChatChannel channel, Component prefix, Component message,
                         @Nullable Function<Player, Component> messageRenderer) {
        super(true);
        this.player = player;
        this.channel = channel;
        this.prefix = prefix;
        this.message = message;
        this.messageRenderer = messageRenderer;
    }

}
