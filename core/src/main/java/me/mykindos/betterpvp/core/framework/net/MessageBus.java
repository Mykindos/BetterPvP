package me.mykindos.betterpvp.core.framework.net;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * How servers talk to each other. Nothing above this knows what carries the messages.
 * <p>
 * Two primitives, because two shapes of question exist: announcing something that every server acts on for itself,
 * and asking something whose answer has to come back. Anything else can be built from those.
 * <p>
 * Handlers are called off the main thread. A handler that touches the world or a player has to schedule that itself.
 */
public interface MessageBus {

    /** Sends to every server, this one included, expecting no reply. */
    void publish(@NotNull BusMessage message);

    /**
     * Asks every server and collects whatever has answered by the time {@code timeout} passes.
     * <p>
     * The result is the replies that arrived, never an error for the ones that did not. A server that is down, busy or
     * uninterested is indistinguishable from one that had nothing to say, so callers decide what silence means.
     */
    @NotNull CompletableFuture<List<BusMessage>> request(@NotNull BusMessage message, @NotNull Duration timeout);

    /** Handles messages on a topic. */
    void subscribe(@NotNull String topic, @NotNull Consumer<BusMessage> handler);

    /**
     * Answers requests on a topic. An empty return says this server has nothing to contribute, which is not the same
     * as a failure and is left unsent.
     */
    void answer(@NotNull String topic, @NotNull Function<BusMessage, Optional<BusMessage>> responder);

    /**
     * Whether a message sent right now would leave this server.
     * <p>
     * A transport is allowed to be unable to send. Callers that need to know whether their message went anywhere ask
     * first, rather than being told by a message that quietly did nothing.
     */
    boolean isAvailable();

    /** Releases whatever the transport holds. A transport that holds nothing does nothing. */
    default void close() {
    }
}
