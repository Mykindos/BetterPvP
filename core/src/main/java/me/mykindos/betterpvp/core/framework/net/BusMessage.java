package me.mykindos.betterpvp.core.framework.net;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * One message on the {@link MessageBus}: a topic, and a map of strings.
 * <p>
 * The payload is deliberately flat rather than an object graph. Every transport can carry string pairs, and a
 * structured payload would have to agree on a serialiser, which is a decision each transport should be free to make
 * for itself.
 */
@Value
@Builder
public class BusMessage {

    /** Keys the bus uses for itself. A payload entry starting with this is overwritten in transit. */
    public static final String RESERVED_PREFIX = "net.";

    /** What the message is about. Handlers register against this. */
    @NotNull String topic;

    /** The server that sent it, filled in by the bus. */
    @Nullable String origin;

    /** Ties a reply to the request that asked for it, and is {@code null} on a plain publish. */
    @Nullable String correlationId;

    @NotNull @Builder.Default Map<String, String> payload = Map.of();

    public @NotNull Optional<String> get(@NotNull String key) {
        return Optional.ofNullable(payload.get(key));
    }

    public @NotNull String getOrDefault(@NotNull String key, @NotNull String fallback) {
        return payload.getOrDefault(key, fallback);
    }

    /** A reply to this message, carrying its correlation so the sender can match it up. */
    public @NotNull BusMessage replyWith(@NotNull Map<String, String> replyPayload) {
        return BusMessage.builder()
                .topic(topic)
                .correlationId(correlationId)
                .payload(replyPayload)
                .build();
    }

    public static @NotNull BusMessage of(@NotNull String topic, @NotNull Map<String, String> payload) {
        return BusMessage.builder().topic(topic).payload(payload).build();
    }

    public static @NotNull BusMessage of(@NotNull String topic, @NotNull String key, @NotNull String value) {
        final Map<String, String> payload = new HashMap<>();
        payload.put(key, value);
        return of(topic, payload);
    }
}
