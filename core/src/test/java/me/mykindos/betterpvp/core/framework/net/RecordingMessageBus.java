package me.mykindos.betterpvp.core.framework.net;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A bus with no network behind it, for testing what a feature puts on the wire and what it does with an arrival.
 * <p>
 * Nothing loops back on its own, so a test says exactly which messages come in through {@link #arrive}.
 */
public class RecordingMessageBus implements MessageBus {

    public final List<BusMessage> published = new ArrayList<>();
    public final List<BusMessage> requested = new ArrayList<>();

    private final Map<String, List<Consumer<BusMessage>>> subscribers = new HashMap<>();
    private final Map<String, List<Function<BusMessage, Optional<BusMessage>>>> responders = new HashMap<>();

    /** What a request answers with. A test sets this to say what the rest of the network had to offer. */
    public List<BusMessage> replies = List.of();

    public boolean available = true;

    @Override
    public void publish(@NotNull BusMessage message) {
        published.add(message);
    }

    @Override
    public @NotNull CompletableFuture<List<BusMessage>> request(@NotNull BusMessage message,
                                                               @NotNull Duration timeout) {
        requested.add(message);
        return CompletableFuture.completedFuture(replies);
    }

    @Override
    public void subscribe(@NotNull String topic, @NotNull Consumer<BusMessage> handler) {
        subscribers.computeIfAbsent(topic, key -> new ArrayList<>()).add(handler);
    }

    @Override
    public void answer(@NotNull String topic, @NotNull Function<BusMessage, Optional<BusMessage>> responder) {
        responders.computeIfAbsent(topic, key -> new ArrayList<>()).add(responder);
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    /** Hands a message to whatever subscribed to its topic, as if another server had sent it. */
    public void arrive(@NotNull BusMessage message) {
        subscribers.getOrDefault(message.getTopic(), List.of()).forEach(handler -> handler.accept(message));
    }

    /** Hands a question to whatever answers its topic, and collects what came back. */
    public @NotNull List<BusMessage> ask(@NotNull BusMessage message) {
        return responders.getOrDefault(message.getTopic(), List.<Function<BusMessage, Optional<BusMessage>>>of())
                .stream()
                .map(responder -> responder.apply(message))
                .flatMap(Optional::stream)
                .toList();
    }
}
