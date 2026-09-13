package me.mykindos.betterpvp.core.framework.net;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.server.CrossServerMessageService;
import me.mykindos.betterpvp.core.framework.server.ServerMessage;
import me.mykindos.betterpvp.core.framework.server.events.ServerMessageReceivedEvent;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Talking to the other servers through the proxy")
class VelocityMessageBusTest extends MessageBusContract {

    private static final String CORRELATION_KEY = "net.correlation";
    private static final String REPLY_KEY = "net.reply";

    private final List<Runnable> scheduled = new ArrayList<>();
    private final List<ServerMessage> broadcast = new ArrayList<>();

    private MockedStatic<Bukkit> bukkitStatic;
    private VelocityMessageBus bus;

    @BeforeEach
    void setUp() {
        final Core core = mock(Core.class);
        when(core.isEnabled()).thenReturn(true);

        final BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTaskLater(any(Core.class), any(Runnable.class), anyLong())).thenAnswer(call -> {
            scheduled.add(call.getArgument(1, Runnable.class));
            return null;
        });

        bukkitStatic = Mockito.mockStatic(Bukkit.class);
        bukkitStatic.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkitStatic.when(Bukkit::getOnlinePlayers).thenReturn(List.of());

        final CrossServerMessageService transport = mock(CrossServerMessageService.class);
        doAnswer(call -> broadcast.add(call.getArgument(0, ServerMessage.class)))
                .when(transport).broadcast(any(ServerMessage.class));

        bus = new VelocityMessageBus(core, transport);
    }

    @AfterEach
    void tearDown() {
        bukkitStatic.close();
    }

    @Override
    protected MessageBus bus() {
        return bus;
    }

    @Override
    protected void arrive(String topic, String origin, String correlationId, boolean reply,
                          Map<String, String> payload) {
        final HashMap<String, String> metadata = new HashMap<>(payload);
        if (correlationId != null) {
            metadata.put(CORRELATION_KEY, correlationId);
        }
        if (reply) {
            metadata.put(REPLY_KEY, "true");
        }

        bus.onServerMessage(new ServerMessageReceivedEvent("BetterPvP",
                ServerMessage.builder().channel(topic).server(origin).metadata(metadata).build()));
    }

    @Override
    protected List<Sent> sent() {
        return broadcast.stream()
                .map(message -> new Sent(message.getChannel(),
                        message.getMetadata().get(CORRELATION_KEY),
                        "true".equals(message.getMetadata().get(REPLY_KEY)),
                        Map.copyOf(message.getMetadata())))
                .toList();
    }

    @Override
    protected void elapse() {
        final List<Runnable> due = List.copyOf(scheduled);
        scheduled.clear();
        due.forEach(Runnable::run);
    }

    @Test
    @DisplayName("the topic travels as the channel and the payload as metadata")
    void wireFormatIsTheServerMessage() {
        bus.publish(BusMessage.of("chat.clan", Map.of("text", "hello")));

        final ServerMessage message = broadcast.get(0);
        assertEquals("chat.clan", message.getChannel());
        assertEquals("hello", message.getMetadata().get("text"));
    }

    @Test
    @DisplayName("wire traffic that did not come from the bus is ignored rather than mistaken for a message")
    void foreignWireTrafficIsIgnored() {
        final List<BusMessage> received = new ArrayList<>();
        bus.subscribe("chat.clan", received::add);

        bus.onServerMessage(new ServerMessageReceivedEvent("BetterPvP",
                ServerMessage.builder().message("something else").build()));

        assertTrue(received.isEmpty());
    }

    @Test
    @DisplayName("the proxy can only carry a message while somebody is online")
    void availabilityFollowsThePlayerCount() {
        assertFalse(bus.isAvailable());
    }
}
