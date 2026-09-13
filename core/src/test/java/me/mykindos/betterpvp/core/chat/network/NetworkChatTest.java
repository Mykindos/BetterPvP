package me.mykindos.betterpvp.core.chat.network;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import me.mykindos.betterpvp.core.chat.events.ChatReceivedEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.search.SearchEngineBase;
import me.mykindos.betterpvp.core.framework.net.BusMessage;
import me.mykindos.betterpvp.core.framework.net.RecordingMessageBus;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Carrying a channel to the servers holding the rest of it")
class NetworkChatTest {

    private static final String HERE = "Clans-1";
    private static final String THERE = "Clans-2";
    private static final String CLAN_ID = "7";

    private final List<ChatReceivedEvent> delivered = new ArrayList<>();

    private Realm originalRealm;
    private MockedStatic<Bukkit> bukkitStatic;
    private RecordingMessageBus bus;
    private NetworkChannels channels;
    private NetworkChat chat;
    private Player sender;
    private Client senderClient;

    /** A channel whose audience is a roster, standing in for clan chat without needing the clans module. */
    private record Roster(@Nullable String key) implements IChatChannel {

        @Override
        public ChatChannel getChannel() {
            return ChatChannel.CLAN;
        }

        @Override
        public Collection<? extends Player> getAudience(@Nullable Player from) {
            return List.of();
        }

        @Override
        public @NotNull Optional<String> getNetworkKey(@NotNull Player from) {
            return Optional.ofNullable(key);
        }
    }

    @BeforeEach
    void setUp() {
        originalRealm = Core.getCurrentRealm();
        Core.setCurrentRealm(new Realm(1, new Server(1, HERE), new Season(1, "test", LocalDate.now())));

        final PluginManager plugins = mock(PluginManager.class);
        Mockito.doAnswer(call -> {
            final Event event = call.getArgument(0, Event.class);
            if (event instanceof ChatReceivedEvent received) {
                delivered.add(received);
            }
            return null;
        }).when(plugins).callEvent(any(Event.class));

        bukkitStatic = Mockito.mockStatic(Bukkit.class);
        bukkitStatic.when(Bukkit::getPluginManager).thenReturn(plugins);

        sender = mock(Player.class);
        when(sender.getUniqueId()).thenReturn(UUID.randomUUID());

        senderClient = mock(Client.class);
        when(senderClient.getName()).thenReturn("Speaker");

        final SearchEngineBase<Client> search = mock(SearchEngineBase.class);
        when(search.offline(any(UUID.class)))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(senderClient)));

        final ClientManager clientManager = mock(ClientManager.class);
        when(clientManager.search()).thenReturn(search);

        bus = new RecordingMessageBus();
        channels = new NetworkChannels();
        chat = new NetworkChat(bus, channels, clientManager);
    }

    @AfterEach
    void tearDown() {
        Core.setCurrentRealm(originalRealm);
        bukkitStatic.close();
    }

    private BusMessage arrival(String origin, String channel, String key, String text) {
        final Map<String, String> payload = new HashMap<>();
        payload.put("channel", channel);
        payload.put("key", key);
        payload.put("sender", UUID.randomUUID().toString());
        payload.put("message", GsonComponentSerializer.gson().serialize(Component.text(text)));
        return BusMessage.builder().topic(NetworkChat.TOPIC).origin(origin).payload(payload).build();
    }

    @Test
    @DisplayName("a channel that reaches nobody elsewhere stays on this server")
    void localOnlyChannelIsNotPublished() {
        chat.relay(sender, new Roster(null), Component.text("Speaker: "), Component.text("hello"));

        assertTrue(bus.published.isEmpty());
    }

    @Test
    @DisplayName("a channel with a roster goes out named by its key")
    void aRosterChannelIsPublished() {
        chat.relay(sender, new Roster(CLAN_ID), Component.text("Speaker: "), Component.text("hello"));

        assertEquals(1, bus.published.size());
        final BusMessage sent = bus.published.getFirst();
        assertEquals(NetworkChat.TOPIC, sent.getTopic());
        assertEquals(ChatChannel.CLAN.name(), sent.getOrDefault("channel", ""));
        assertEquals(CLAN_ID, sent.getOrDefault("key", ""));
        assertEquals(sender.getUniqueId().toString(), sent.getOrDefault("sender", ""));
    }

    @Test
    @DisplayName("nothing is sent when the bus has nowhere to send it")
    void nothingLeavesAnUnavailableBus() {
        bus.available = false;

        chat.relay(sender, new Roster(CLAN_ID), Component.text("Speaker: "), Component.text("hello"));

        assertTrue(bus.published.isEmpty());
    }

    @Test
    @DisplayName("a message this server sent is not delivered back to it")
    void ownMessagesAreIgnored() {
        channels.register(ChatChannel.CLAN, key -> List.of(mock(Player.class)));

        bus.arrive(arrival(HERE, ChatChannel.CLAN.name(), CLAN_ID, "hello"));

        assertTrue(delivered.isEmpty(), "the local audience already heard it as it was spoken");
    }

    @Test
    @DisplayName("an arrival reaches the audience the channel's owner names")
    void anArrivalReachesTheResolvedAudience() {
        final Player listener = mock(Player.class);
        channels.register(ChatChannel.CLAN, key -> CLAN_ID.equals(key) ? List.of(listener) : List.of());

        bus.arrive(arrival(THERE, ChatChannel.CLAN.name(), CLAN_ID, "hello"));

        assertEquals(1, delivered.size());
        final ChatReceivedEvent received = delivered.getFirst();
        assertEquals(listener, received.getTarget());
        assertEquals(senderClient, received.getClient());
        assertEquals(ChatChannel.CLAN, received.getChannel());
        assertEquals(Component.text("hello"), received.getMessage());
    }

    @Test
    @DisplayName("the sender is absent, since they are on the server the message came from")
    void theSenderIsNotAPlayerHere() {
        channels.register(ChatChannel.CLAN, key -> List.of(mock(Player.class)));

        bus.arrive(arrival(THERE, ChatChannel.CLAN.name(), CLAN_ID, "hello"));

        assertNull(delivered.getFirst().getPlayer(), "only the sender's client is known on this side");
    }

    @Test
    @DisplayName("a channel nobody claimed here reaches nobody")
    void anUnclaimedChannelReachesNobody() {
        bus.arrive(arrival(THERE, ChatChannel.ALLIANCE.name(), CLAN_ID, "hello"));

        assertTrue(delivered.isEmpty());
    }

    @Test
    @DisplayName("a channel that no longer exists is dropped rather than throwing")
    void anUnknownChannelIsDropped() {
        bus.arrive(arrival(THERE, "RETIRED_CHANNEL", CLAN_ID, "hello"));

        assertTrue(delivered.isEmpty());
    }
}
