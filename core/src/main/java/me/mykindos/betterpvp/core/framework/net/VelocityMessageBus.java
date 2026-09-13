package me.mykindos.betterpvp.core.framework.net;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.server.CrossServerMessageService;
import me.mykindos.betterpvp.core.framework.server.ServerMessage;
import me.mykindos.betterpvp.core.framework.server.events.ServerMessageReceivedEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A {@link MessageBus} carried by the proxy, over the plugin messaging channel the server already speaks.
 * <p>
 * The proxy forwards a message to every backend except the one that sent it, so this delivers locally itself and lets
 * the forward cover the rest. That is also why it needs somebody online: a plugin message travels attached to a
 * player's connection, and with nobody on this server there is nothing to attach it to.
 */
@Singleton
@BPvPListener
@CustomLog
public class VelocityMessageBus implements MessageBus, Listener {

    /** Marks a message as an answer rather than a question, so a responder does not answer its own reply. */
    private static final String REPLY_KEY = BusMessage.RESERVED_PREFIX + "reply";
    private static final String CORRELATION_KEY = BusMessage.RESERVED_PREFIX + "correlation";

    private final Core core;
    private final CrossServerMessageService transport;

    private final Map<String, List<Consumer<BusMessage>>> subscribers = new ConcurrentHashMap<>();
    private final Map<String, List<Function<BusMessage, Optional<BusMessage>>>> responders = new ConcurrentHashMap<>();
    private final Map<String, List<BusMessage>> awaitedReplies = new ConcurrentHashMap<>();

    @Inject
    public VelocityMessageBus(@NotNull Core core, @NotNull CrossServerMessageService transport) {
        this.core = core;
        this.transport = transport;
    }

    @Override
    public void publish(@NotNull BusMessage message) {
        transport.broadcast(toWire(message));
    }

    @Override
    public @NotNull CompletableFuture<List<BusMessage>> request(@NotNull BusMessage message,
                                                                @NotNull Duration timeout) {
        final String correlationId = UUID.randomUUID().toString();
        final List<BusMessage> replies = new CopyOnWriteArrayList<>();
        awaitedReplies.put(correlationId, replies);

        final CompletableFuture<List<BusMessage>> answered = new CompletableFuture<>();
        UtilServer.runTaskLater(core, () -> {
            awaitedReplies.remove(correlationId);
            answered.complete(List.copyOf(replies));
        }, Math.max(1L, timeout.toMillis() / 50L));

        transport.broadcast(toWire(BusMessage.builder()
                .topic(message.getTopic())
                .correlationId(correlationId)
                .payload(message.getPayload())
                .build()));

        return answered;
    }

    @Override
    public void subscribe(@NotNull String topic, @NotNull Consumer<BusMessage> handler) {
        subscribers.computeIfAbsent(topic, key -> new CopyOnWriteArrayList<>()).add(handler);
    }

    @Override
    public void answer(@NotNull String topic, @NotNull Function<BusMessage, Optional<BusMessage>> responder) {
        responders.computeIfAbsent(topic, key -> new CopyOnWriteArrayList<>()).add(responder);
    }

    @Override
    public boolean isAvailable() {
        return !Bukkit.getOnlinePlayers().isEmpty();
    }

    @EventHandler
    public void onServerMessage(@NotNull ServerMessageReceivedEvent event) {
        final BusMessage message = fromWire(event.getMessage());
        if (message == null) {
            return;
        }

        if (isReply(event.getMessage())) {
            final List<BusMessage> waiting = message.getCorrelationId() == null
                    ? null : awaitedReplies.get(message.getCorrelationId());
            if (waiting != null) {
                waiting.add(message);
            }
            return;
        }

        subscribers.getOrDefault(message.getTopic(), List.of())
                .forEach(handler -> handle(message, handler));

        if (message.getCorrelationId() != null) {
            responders.getOrDefault(message.getTopic(), List.of())
                    .forEach(responder -> respond(message, responder));
        }
    }

    private void handle(@NotNull BusMessage message, @NotNull Consumer<BusMessage> handler) {
        try {
            handler.accept(message);
        } catch (RuntimeException exception) {
            log.error("Handler for topic '{}' failed", message.getTopic(), exception).submit();
        }
    }

    private void respond(@NotNull BusMessage message,
                         @NotNull Function<BusMessage, Optional<BusMessage>> responder) {
        try {
            responder.apply(message).ifPresent(reply -> {
                final ServerMessage wire = toWire(reply);
                wire.getMetadata().put(REPLY_KEY, "true");
                transport.broadcast(wire);
            });
        } catch (RuntimeException exception) {
            log.error("Responder for topic '{}' failed", message.getTopic(), exception).submit();
        }
    }

    private boolean isReply(@NotNull ServerMessage wire) {
        return wire.getMetadata() != null && "true".equals(wire.getMetadata().get(REPLY_KEY));
    }

    private @NotNull ServerMessage toWire(@NotNull BusMessage message) {
        final HashMap<String, String> metadata = new HashMap<>();
        message.getPayload().forEach((key, value) -> {
            if (!key.startsWith(BusMessage.RESERVED_PREFIX)) {
                metadata.put(key, value);
            }
        });
        if (message.getCorrelationId() != null) {
            metadata.put(CORRELATION_KEY, message.getCorrelationId());
        }

        return ServerMessage.builder()
                .channel(message.getTopic())
                .metadata(metadata)
                .build();
    }

    /** @return the message, or {@code null} for wire traffic that did not come from this bus */
    private BusMessage fromWire(@NotNull ServerMessage wire) {
        if (wire.getChannel() == null) {
            return null;
        }

        final Map<String, String> payload = new HashMap<>();
        final Map<String, String> metadata = wire.getMetadata();
        if (metadata != null) {
            metadata.forEach((key, value) -> {
                if (!key.startsWith(BusMessage.RESERVED_PREFIX)) {
                    payload.put(key, value);
                }
            });
        }

        return BusMessage.builder()
                .topic(wire.getChannel())
                .origin(wire.getServer())
                .correlationId(metadata == null ? null : metadata.get(CORRELATION_KEY))
                .payload(payload)
                .build();
    }
}
