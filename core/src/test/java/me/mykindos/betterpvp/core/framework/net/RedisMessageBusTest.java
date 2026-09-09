package me.mykindos.betterpvp.core.framework.net;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import redis.clients.jedis.JedisPool;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Talking to the other servers through Redis")
class RedisMessageBusTest extends MessageBusContract {

    private final List<Runnable> scheduled = new ArrayList<>();
    private final List<RedisEnvelope> published = new ArrayList<>();

    private Realm originalRealm;
    private MockedStatic<Bukkit> bukkitStatic;
    private RedisMessageBus bus;

    /** The bus with the network taken out, so the contract runs against its routing rather than against a server. */
    private class CapturingBus extends RedisMessageBus {
        CapturingBus(Core core) {
            super(core, mock(JedisPool.class), "betterpvp:test");
        }

        @Override
        protected void send(RedisEnvelope envelope) {
            published.add(envelope);
        }
    }

    @BeforeEach
    void setUp() {
        originalRealm = Core.getCurrentRealm();
        Core.setCurrentRealm(new Realm(1, new Server(1, "Clans-1"), new Season(1, "test", LocalDate.now())));

        final Core core = mock(Core.class);
        when(core.isEnabled()).thenReturn(true);

        final BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTaskLater(any(Core.class), any(Runnable.class), anyLong())).thenAnswer(call -> {
            scheduled.add(call.getArgument(1, Runnable.class));
            return null;
        });

        bukkitStatic = Mockito.mockStatic(Bukkit.class);
        bukkitStatic.when(Bukkit::getScheduler).thenReturn(scheduler);

        bus = new CapturingBus(core);
    }

    @AfterEach
    void tearDown() {
        bus.close();
        Core.setCurrentRealm(originalRealm);
        bukkitStatic.close();
    }

    @Override
    protected MessageBus bus() {
        return bus;
    }

    @Override
    protected void arrive(String topic, String origin, String correlationId, boolean reply,
                          Map<String, String> payload) {
        bus.deliver(new RedisEnvelope(topic, origin, correlationId, reply, payload));
    }

    @Override
    protected List<Sent> sent() {
        return published.stream()
                .map(envelope -> new Sent(envelope.getTopic(), envelope.getCorrelationId(), envelope.isReply(),
                        envelope.getPayload()))
                .toList();
    }

    @Override
    protected void elapse() {
        final List<Runnable> due = List.copyOf(scheduled);
        scheduled.clear();
        due.forEach(Runnable::run);
    }

    @Test
    @DisplayName("a sent message names the server it came from, so a reply can tell who answered")
    void outgoingMessagesCarryTheirOrigin() {
        bus.publish(BusMessage.of("chat.clan", Map.of("text", "hello")));

        assertEquals("Clans-1", published.get(0).getOrigin());
    }
}
