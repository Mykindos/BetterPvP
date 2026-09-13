package me.mykindos.betterpvp.core.chat.network;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import me.mykindos.betterpvp.core.chat.events.ChatReceivedEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.net.BusMessage;
import me.mykindos.betterpvp.core.framework.net.MessageBus;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Carries a channel that has a roster to the other servers holding part of it.
 * <p>
 * What travels is the text and who said it, not a finished line. Every server renders for itself, so a clan message
 * arriving from elsewhere still picks up the clan colours, the skill hovers and the recipient's ignore list, exactly
 * as one spoken here would.
 */
@Singleton
@CustomLog
public class NetworkChat {

    static final String TOPIC = "chat.channel";

    private static final String CHANNEL = "channel";
    private static final String KEY = "key";
    private static final String SENDER = "sender";
    private static final String PREFIX = "prefix";
    private static final String MESSAGE = "message";

    private final MessageBus bus;
    private final NetworkChannels channels;
    private final ClientManager clientManager;

    @Inject
    public NetworkChat(@NotNull MessageBus bus, @NotNull NetworkChannels channels,
                       @NotNull ClientManager clientManager) {
        this.bus = bus;
        this.channels = channels;
        this.clientManager = clientManager;
        bus.subscribe(TOPIC, this::receive);
    }

    /**
     * Sends what was just said to the rest of the network, if the channel reaches that far.
     *
     * @param message the filtered text, since the servers receiving it will not filter it again
     */
    public void relay(@NotNull Player sender, @NotNull IChatChannel channel, @NotNull Component prefix,
                      @NotNull Component message) {
        final Optional<String> key = channel.getNetworkKey(sender);
        if (key.isEmpty() || !bus.isAvailable()) {
            return;
        }

        final Map<String, String> payload = new HashMap<>();
        payload.put(CHANNEL, channel.getChannel().name());
        payload.put(KEY, key.get());
        payload.put(SENDER, sender.getUniqueId().toString());
        payload.put(PREFIX, GsonComponentSerializer.gson().serialize(prefix));
        payload.put(MESSAGE, GsonComponentSerializer.gson().serialize(message));
        bus.publish(BusMessage.of(TOPIC, payload));
    }

    /** Hands an arrival to the ordinary chat pipeline, once per recipient this server holds. */
    private void receive(@NotNull BusMessage incoming) {
        if (Core.getCurrentRealm().getServer().getName().equals(incoming.getOrigin())) {
            return;
        }

        final Optional<ChatChannel> channel = channelOf(incoming.getOrDefault(CHANNEL, ""));
        final Optional<String> senderId = incoming.get(SENDER);
        if (channel.isEmpty() || senderId.isEmpty()) {
            return;
        }

        final Collection<? extends Player> audience =
                channels.audienceFor(channel.get(), incoming.getOrDefault(KEY, ""));
        if (audience.isEmpty()) {
            return;
        }

        final Component prefix = deserialize(incoming.get(PREFIX));
        final Component message = deserialize(incoming.get(MESSAGE));

        clientManager.search().offline(UUID.fromString(senderId.get())).thenAccept(found -> {
            final Client sender = found.orElse(null);
            if (sender == null) {
                return;
            }

            for (Player target : audience) {
                UtilServer.callEvent(new ChatReceivedEvent(null, sender, target, channel.get(), prefix, message));
            }
        });
    }

    private @NotNull Optional<ChatChannel> channelOf(@NotNull String name) {
        try {
            return Optional.of(ChatChannel.valueOf(name));
        } catch (IllegalArgumentException unknown) {
            log.warn("A message arrived for the unknown channel '{}'", name).submit();
            return Optional.empty();
        }
    }

    private @NotNull Component deserialize(@NotNull Optional<String> json) {
        return json.map(GsonComponentSerializer.gson()::deserialize).orElse(Component.empty());
    }
}
