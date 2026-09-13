package me.mykindos.betterpvp.core.chat.network;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * How a server works out who here should hear something said on another server.
 * <p>
 * The sender is not on this machine, so the audience cannot be asked of the channel object that produced the message.
 * It is answered instead from the key the message carries, by whichever module owns that channel. A channel with
 * nothing registered stays on the server it was spoken on.
 */
@Singleton
public class NetworkChannels {

    private final Map<ChatChannel, Function<String, Collection<? extends Player>>> resolvers = new ConcurrentHashMap<>();

    /**
     * @param audience turns the key on a message into the local players who should receive it
     */
    public void register(@NotNull ChatChannel channel,
                         @NotNull Function<String, Collection<? extends Player>> audience) {
        resolvers.put(channel, audience);
    }

    public @NotNull Collection<? extends Player> audienceFor(@NotNull ChatChannel channel, @NotNull String key) {
        final Function<String, Collection<? extends Player>> audience = resolvers.get(channel);
        return audience == null ? List.of() : audience.apply(key);
    }
}
