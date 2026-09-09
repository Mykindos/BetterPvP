package me.mykindos.betterpvp.core.framework.net;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every {@link MessageBus} has to do, whatever carries the messages. Each transport runs this suite, which is
 * the only thing that makes the two interchangeable rather than merely similar.
 */
abstract class MessageBusContract {

    /** One message this bus put on the wire. */
    record Sent(String topic, String correlationId, boolean reply, Map<String, String> payload) {
    }

    protected abstract MessageBus bus();

    /** Hands the bus a message as though another server had sent it. */
    protected abstract void arrive(String topic, String origin, String correlationId, boolean reply,
                                   Map<String, String> payload);

    /** Everything the bus has sent so far, oldest first. */
    protected abstract List<Sent> sent();

    /** Runs whatever the bus scheduled, standing in for a timeout elapsing. */
    protected abstract void elapse();

    private Sent lastSent() {
        final List<Sent> all = sent();
        assertFalse(all.isEmpty(), "expected the bus to have sent something");
        return all.get(all.size() - 1);
    }

    @Test
    @DisplayName("publishing puts the topic and payload on the wire")
    void publishSends() {
        bus().publish(BusMessage.of("chat.clan", Map.of("clan", "42", "text", "hello")));

        final Sent message = lastSent();
        assertEquals("chat.clan", message.topic());
        assertEquals("42", message.payload().get("clan"));
        assertEquals("hello", message.payload().get("text"));
        assertFalse(message.reply());
    }

    @Test
    @DisplayName("a message that arrives reaches the handlers for its topic, tagged with where it came from")
    void arrivingMessageReachesSubscribers() {
        final List<BusMessage> received = new ArrayList<>();
        bus().subscribe("chat.clan", received::add);

        arrive("chat.clan", "Clans-2", null, false, Map.of("text", "hello"));

        assertEquals(1, received.size());
        assertEquals("Clans-2", received.get(0).getOrigin());
        assertEquals(Optional.of("hello"), received.get(0).get("text"));
    }

    @Test
    @DisplayName("a message on another topic is not delivered")
    void otherTopicsAreIgnored() {
        final List<BusMessage> received = new ArrayList<>();
        bus().subscribe("chat.clan", received::add);

        arrive("leaderboard.query", "Clans-2", null, false, Map.of());

        assertTrue(received.isEmpty());
    }

    @Test
    @DisplayName("a request collects the answers that arrive before the timeout")
    void requestCollectsReplies() {
        final CompletableFuture<List<BusMessage>> answered =
                bus().request(BusMessage.of("player.find", "name", "Tester"), Duration.ofSeconds(1));

        final String correlationId = lastSent().correlationId();
        assertNotNull(correlationId);
        assertFalse(answered.isDone());

        arrive("player.find", "Clans-2", correlationId, true, Map.of("server", "Clans-2"));
        elapse();

        assertEquals(1, answered.join().size());
        assertEquals(Optional.of("Clans-2"), answered.join().get(0).get("server"));
    }

    @Test
    @DisplayName("a request nobody answers completes empty rather than failing")
    void requestWithoutRepliesCompletesEmpty() {
        final CompletableFuture<List<BusMessage>> answered =
                bus().request(BusMessage.of("player.find", "name", "Tester"), Duration.ofSeconds(1));

        elapse();

        assertTrue(answered.join().isEmpty());
    }

    @Test
    @DisplayName("a reply for a request that already timed out is dropped")
    void lateReplyIsDropped() {
        final CompletableFuture<List<BusMessage>> answered =
                bus().request(BusMessage.of("player.find", "name", "Tester"), Duration.ofSeconds(1));
        final String correlationId = lastSent().correlationId();
        elapse();

        arrive("player.find", "Clans-2", correlationId, true, Map.of("server", "Clans-2"));

        assertTrue(answered.join().isEmpty());
    }

    @Test
    @DisplayName("a responder answers a question and its answer is marked as one")
    void responderReplies() {
        bus().answer("player.find", request -> Optional.of(request.replyWith(Map.of("server", "Clans-1"))));

        arrive("player.find", "Clans-2", "abc", false, Map.of("name", "Tester"));

        final Sent reply = lastSent();
        assertEquals("player.find", reply.topic());
        assertTrue(reply.reply());
        assertEquals("abc", reply.correlationId());
        assertEquals("Clans-1", reply.payload().get("server"));
    }

    @Test
    @DisplayName("a responder is not asked to answer somebody else's answer")
    void responderIgnoresReplies() {
        bus().answer("player.find", request -> Optional.of(request.replyWith(Map.of("server", "Clans-1"))));

        arrive("player.find", "Clans-2", "abc", true, Map.of());

        assertTrue(sent().isEmpty());
    }

    @Test
    @DisplayName("a responder with nothing to say says nothing")
    void silentResponderSendsNothing() {
        bus().answer("player.find", request -> Optional.empty());

        arrive("player.find", "Clans-2", "abc", false, Map.of("name", "Tester"));

        assertTrue(sent().isEmpty());
    }

    @Test
    @DisplayName("a plain publish is not treated as a question")
    void publishDoesNotTriggerResponders() {
        bus().answer("player.find", request -> Optional.of(request.replyWith(Map.of("server", "Clans-1"))));

        arrive("player.find", "Clans-2", null, false, Map.of("name", "Tester"));

        assertTrue(sent().isEmpty());
    }

    @Test
    @DisplayName("a caller cannot spoof the bus's own fields through the payload")
    void reservedKeysAreStripped() {
        bus().publish(BusMessage.of("chat.clan", Map.of("net.reply", "true", "text", "hello")));

        final Sent message = lastSent();
        assertFalse(message.payload().containsKey("net.reply"));
        assertFalse(message.reply());
        assertEquals("hello", message.payload().get("text"));
    }
}
