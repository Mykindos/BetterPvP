package me.mykindos.betterpvp.core.chat.network;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.filter.IFilterService;
import me.mykindos.betterpvp.core.chat.ignore.IIgnoreService;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.framework.net.BusMessage;
import me.mykindos.betterpvp.core.framework.net.RecordingMessageBus;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import me.mykindos.betterpvp.core.utilities.search.SearchEngineBase;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jetbrains.annotations.NotNull;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Messaging one player wherever they are")
class PrivateMessagesTest {

    private static final String HERE = "Clans-1";
    private static final String THERE = "Clans-2";

    private final Map<UUID, Client> clientsById = new HashMap<>();
    private final Map<String, Client> clientsByName = new HashMap<>();
    private final Map<UUID, Player> onlineHere = new HashMap<>();

    private Realm originalRealm;
    private MockedStatic<Bukkit> bukkitStatic;
    private RecordingMessageBus bus;
    private IIgnoreService ignoreService;
    private PrivateMessages messages;

    /** Who reads as offline, standing in for vanish so the effect registry stays out of the test. */
    private final Set<Player> hidden = new HashSet<>();

    private Player sender;
    private Client senderClient;
    private Client targetClient;

    @BeforeEach
    void setUp() {
        originalRealm = Core.getCurrentRealm();
        Core.setCurrentRealm(new Realm(1, new Server(1, HERE), new Season(1, "test", LocalDate.now())));

        bukkitStatic = Mockito.mockStatic(Bukkit.class);
        bukkitStatic.when(() -> Bukkit.getPlayer(any(UUID.class)))
                .thenAnswer(call -> onlineHere.get(call.getArgument(0, UUID.class)));
        bukkitStatic.when(Bukkit::getOnlinePlayers).thenAnswer(call -> List.copyOf(onlineHere.values()));

        senderClient = client("Speaker");
        targetClient = client("Listener");
        sender = online(senderClient);

        final SearchEngineBase<Client> search = mock(SearchEngineBase.class);
        when(search.offline(any(UUID.class)))
                .thenAnswer(call -> CompletableFuture.completedFuture(
                        Optional.ofNullable(clientsById.get(call.getArgument(0, UUID.class)))));
        when(search.offline(anyString()))
                .thenAnswer(call -> CompletableFuture.completedFuture(
                        Optional.ofNullable(clientsByName.get(call.getArgument(0, String.class)))));
        when(search.online(any(Player.class)))
                .thenAnswer(call -> clientsById.get(call.getArgument(0, Player.class).getUniqueId()));

        final ClientManager clientManager = mock(ClientManager.class);
        when(clientManager.search()).thenReturn(search);

        final IFilterService filterService = mock(IFilterService.class);
        when(filterService.filterMessage(anyString()))
                .thenAnswer(call -> CompletableFuture.completedFuture(call.getArgument(0, String.class)));

        ignoreService = mock(IIgnoreService.class);
        bus = new RecordingMessageBus();
        messages = new PrivateMessages(bus, clientManager, filterService, ignoreService,
                mock(EffectManager.class)) {
            @Override
            protected boolean isHidden(@NotNull Player target, @NotNull Client senderClient) {
                return hidden.contains(target);
            }
        };
    }

    @AfterEach
    void tearDown() {
        Core.setCurrentRealm(originalRealm);
        bukkitStatic.close();
    }

    private Client client(String name) {
        final UUID id = UUID.randomUUID();
        final Client client = mock(Client.class);
        when(client.getUniqueId()).thenReturn(id);
        when(client.getName()).thenReturn(name);
        clientsById.put(id, client);
        clientsByName.put(name, client);
        return client;
    }

    /** Puts the client's player on this server. */
    private Player online(Client client) {
        final UUID id = client.getUniqueId();
        final String name = client.getName();
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getName()).thenReturn(name);
        onlineHere.put(id, player);
        return player;
    }

    private PrivateMessages.Result send(String targetName) {
        return messages.send(sender, senderClient, targetName, "hello").join();
    }

    @Test
    @DisplayName("a name nobody has been seen under is not found")
    void anUnknownNameIsNotFound() {
        assertEquals(PrivateMessages.Outcome.NOT_FOUND, send("Nobody").getOutcome());
        assertTrue(bus.requested.isEmpty(), "there is no point asking the network about a name it never knew");
    }

    @Test
    @DisplayName("messaging yourself is refused")
    void messagingYourselfIsRefused() {
        assertEquals(PrivateMessages.Outcome.SELF, send("Speaker").getOutcome());
    }

    @Test
    @DisplayName("somebody you ignore is not messaged, and is named in the answer")
    void ignoringTheRecipientStopsIt() {
        when(ignoreService.isClientIgnored(senderClient, targetClient)).thenReturn(true);

        final PrivateMessages.Result result = send("Listener");

        assertEquals(PrivateMessages.Outcome.IGNORING, result.getOutcome());
        assertEquals("Listener", result.getTargetName());
    }

    @Test
    @DisplayName("a recipient on this server is written to directly")
    void aLocalRecipientIsWrittenToDirectly() {
        final Player target = online(targetClient);

        assertEquals(PrivateMessages.Outcome.DELIVERED, send("Listener").getOutcome());
        verify(target).sendMessage(any(Component.class));
        verify(sender).sendMessage(any(Component.class));
        assertTrue(bus.requested.isEmpty(), "both of them are here, so nothing needs to leave");
    }

    @Test
    @DisplayName("a recipient who ignores the sender reads nothing, and the sender is not told")
    void theRecipientsIgnoreIsInvisibleToTheSender() {
        final Player target = online(targetClient);
        when(ignoreService.isClientIgnored(targetClient, senderClient)).thenReturn(true);

        assertEquals(PrivateMessages.Outcome.DELIVERED, send("Listener").getOutcome());
        verify(target, never()).sendMessage(any(Component.class));
        verify(sender).sendMessage(any(Component.class));
    }

    @Test
    @DisplayName("a hidden recipient reads as offline rather than being messaged")
    void aHiddenRecipientReadsAsOffline() {
        hidden.add(online(targetClient));

        assertEquals(PrivateMessages.Outcome.NOT_FOUND, send("Listener").getOutcome());
        assertTrue(bus.requested.isEmpty(), "they are here, so the network is not asked about them either");
    }

    @Test
    @DisplayName("a recipient who is not here is asked for across the network")
    void anAbsentRecipientIsAskedForAcrossTheNetwork() {
        bus.replies = List.of(BusMessage.of(PrivateMessages.TOPIC, "target",
                targetClient.getUniqueId().toString()));

        assertEquals(PrivateMessages.Outcome.DELIVERED, send("Listener").getOutcome());
        assertEquals(1, bus.requested.size());

        final BusMessage asked = bus.requested.getFirst();
        assertEquals(targetClient.getUniqueId().toString(), asked.getOrDefault("target", ""));
        assertEquals("hello", asked.getOrDefault("message", ""));
        verify(sender).sendMessage(any(Component.class));
    }

    @Test
    @DisplayName("silence from the network means nobody has them")
    void silenceMeansNotFound() {
        bus.replies = List.of();

        assertEquals(PrivateMessages.Outcome.NOT_FOUND, send("Listener").getOutcome());
        verify(sender, never()).sendMessage(any(Component.class));
    }

    @Test
    @DisplayName("with no bus, a recipient who is not on this server simply is not found")
    void withoutABusOnlyThisServerIsReachable() {
        bus.available = false;

        assertEquals(PrivateMessages.Outcome.NOT_FOUND, send("Listener").getOutcome());
        assertTrue(bus.requested.isEmpty());
    }

    @Test
    @DisplayName("a question about somebody this server does not hold goes unanswered")
    void aQuestionAboutAnAbsentPlayerIsNotAnswered() {
        assertTrue(bus.ask(question(THERE, targetClient.getUniqueId())).isEmpty());
    }

    @Test
    @DisplayName("the server holding the recipient delivers and says so")
    void theHoldingServerDeliversAndAnswers() {
        final Player target = online(targetClient);

        final List<BusMessage> answers = bus.ask(question(THERE, targetClient.getUniqueId()));

        assertEquals(1, answers.size());
        verify(target).sendMessage(any(Component.class));
    }

    @Test
    @DisplayName("a question this server asked is not answered by this server")
    void ourOwnQuestionIsNotAnsweredHere() {
        final Player target = online(targetClient);

        assertTrue(bus.ask(question(HERE, targetClient.getUniqueId())).isEmpty());
        verify(target, never()).sendMessage(any(Component.class));
    }

    @Test
    @DisplayName("administrating staff on this server read what passed through it")
    void staffHereSeeWhatPassedThrough() {
        final Client staffClient = client("Watcher");
        final Player staff = online(staffClient);
        when(staffClient.isAdministrating()).thenReturn(true);
        final Player target = online(targetClient);

        assertEquals(PrivateMessages.Outcome.DELIVERED, send("Listener").getOutcome());

        verify(staff).sendMessage(any(Component.class));
        assertFalse(onlineHere.isEmpty());
        verify(target).sendMessage(any(Component.class));
    }

    private BusMessage question(String origin, UUID target) {
        final Map<String, String> payload = new HashMap<>();
        payload.put("sender", senderClient.getUniqueId().toString());
        payload.put("senderName", "Speaker");
        payload.put("target", target.toString());
        payload.put("message", "hello");
        return BusMessage.builder().topic(PrivateMessages.TOPIC).origin(origin).payload(payload).build();
    }
}
