package me.mykindos.betterpvp.core.framework.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

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
 * A {@link MessageBus} over Redis pub/sub. The transport of record once it is deployed, since it needs no player to
 * carry a message and no proxy to route one.
 * <p>
 * Everything travels on a single channel with the topic inside the message, rather than a Redis channel per topic.
 * One channel means one subscription whose lifetime matches the connection, where per-topic channels would have to be
 * added and removed as handlers register and re-added on every reconnect. Sorting by topic on arrival costs a map
 * lookup, which is not worth that.
 * <p>
 * Redis delivers a published message to every subscriber including this server's own, so a publish reaches the local
 * handlers without being delivered twice.
 */
@CustomLog
public class RedisMessageBus implements MessageBus {

    private static final int RECONNECT_DELAY_MILLIS = 5_000;

    private final Core core;
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, List<Consumer<BusMessage>>> subscribers = new ConcurrentHashMap<>();
    private final Map<String, List<Function<BusMessage, Optional<BusMessage>>>> responders = new ConcurrentHashMap<>();
    private final Map<String, List<BusMessage>> awaitedReplies = new ConcurrentHashMap<>();

    private final String channel;
    private final JedisPool pool;

    private volatile boolean closed;
    private volatile JedisPubSub subscription;
    private Thread listener;

    public RedisMessageBus(@NotNull Core core, @NotNull JedisPool pool, @NotNull String channel) {
        this.core = core;
        this.pool = pool;
        this.channel = channel;
    }

    /** Opens the subscription. Separate from construction so nothing subscribes before the connection is proved. */
    public void start() {
        listen();
    }

    @Override
    public void publish(@NotNull BusMessage message) {
        send(envelope(message, false));
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

        send(envelope(BusMessage.builder()
                .topic(message.getTopic())
                .correlationId(correlationId)
                .payload(message.getPayload())
                .build(), false));

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
        return !closed && !pool.isClosed();
    }

    @Override
    public void close() {
        closed = true;

        final JedisPubSub current = subscription;
        if (current != null && current.isSubscribed()) {
            current.unsubscribe();
        }
        if (listener != null) {
            listener.interrupt();
        }
        // The pool is shared with the directory, and closed by the connection that owns it.
    }

    /**
     * Publishes off the main thread. A publish is a round trip to another machine, and the callers are game code that
     * has no business waiting on one.
     */
    protected void send(@NotNull RedisEnvelope envelope) {
        if (closed) {
            return;
        }

        UtilServer.runTaskAsync(core, () -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.publish(channel, mapper.writeValueAsString(envelope));
            } catch (Exception exception) {
                log.error("Failed to publish on topic '{}'", envelope.getTopic(), exception).submit();
            }
        });
    }

    /**
     * Holds the subscription open on its own thread, reconnecting until closed.
     * <p>
     * Jedis blocks the calling thread for as long as it is subscribed, so this cannot share a thread with anything
     * else, and a dropped connection surfaces as the call returning rather than as an event.
     */
    private void listen() {
        listener = new Thread(() -> {
            while (!closed) {
                try (Jedis jedis = pool.getResource()) {
                    subscription = new Receiver();
                    jedis.subscribe(subscription, channel);
                } catch (Exception exception) {
                    if (!closed) {
                        log.warn("Lost the Redis subscription, retrying in {}ms", RECONNECT_DELAY_MILLIS, exception).submit();
                    }
                }

                if (!closed) {
                    try {
                        Thread.sleep(RECONNECT_DELAY_MILLIS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, "BetterPvP Redis Bus");

        listener.setDaemon(true);
        listener.start();
    }

    private @NotNull RedisEnvelope envelope(@NotNull BusMessage message, boolean reply) {
        final Map<String, String> payload = new HashMap<>();
        message.getPayload().forEach((key, value) -> {
            if (!key.startsWith(BusMessage.RESERVED_PREFIX)) {
                payload.put(key, value);
            }
        });

        return new RedisEnvelope(message.getTopic(), Core.getCurrentRealm().getServer().getName(),
                message.getCorrelationId(), reply, payload);
    }

    /** Routes one arrival to whatever registered for its topic. Package-private so the routing can be tested. */
    void deliver(@NotNull RedisEnvelope envelope) {
        final BusMessage message = BusMessage.builder()
                .topic(envelope.getTopic())
                .origin(envelope.getOrigin())
                .correlationId(envelope.getCorrelationId())
                .payload(envelope.getPayload() == null ? Map.of() : Map.copyOf(envelope.getPayload()))
                .build();

        if (envelope.isReply()) {
            final List<BusMessage> waiting = message.getCorrelationId() == null
                    ? null : awaitedReplies.get(message.getCorrelationId());
            if (waiting != null) {
                waiting.add(message);
            }
            return;
        }

        for (Consumer<BusMessage> handler : subscribers.getOrDefault(message.getTopic(), List.of())) {
            try {
                handler.accept(message);
            } catch (RuntimeException exception) {
                log.error("Handler for topic '{}' failed", message.getTopic(), exception).submit();
            }
        }

        if (message.getCorrelationId() == null) {
            return;
        }

        for (Function<BusMessage, Optional<BusMessage>> responder : responders.getOrDefault(message.getTopic(), List.of())) {
            try {
                responder.apply(message).ifPresent(reply -> send(envelope(reply, true)));
            } catch (RuntimeException exception) {
                log.error("Responder for topic '{}' failed", message.getTopic(), exception).submit();
            }
        }
    }

    private class Receiver extends JedisPubSub {

        @Override
        public void onMessage(String incoming, String body) {
            try {
                deliver(mapper.readValue(body, RedisEnvelope.class));
            } catch (Exception exception) {
                log.error("Could not read a message off channel '{}'", incoming, exception).submit();
            }
        }
    }
}
