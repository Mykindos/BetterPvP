package me.mykindos.betterpvp.core.stats;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.net.BusMessage;
import me.mykindos.betterpvp.core.framework.net.RecordingMessageBus;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Keeping a standing current across the network")
class LeaderboardSyncTest {

    private static final String HERE = "Clans-1";
    private static final String THERE = "Clans-2";
    private static final String NAME = "Combat";

    private Realm originalRealm;
    private RecordingMessageBus bus;
    private LeaderboardManager leaderboardManager;
    private Leaderboard<?, ?> leaderboard;
    private LeaderboardSync sync;

    @BeforeEach
    void setUp() {
        originalRealm = Core.getCurrentRealm();
        Core.setCurrentRealm(new Realm(1, new Server(1, HERE), new Season(1, "test", LocalDate.now())));

        leaderboard = mock(Leaderboard.class);
        when(leaderboard.getName()).thenReturn(NAME);

        leaderboardManager = mock(LeaderboardManager.class);
        when(leaderboardManager.getObject(anyString())).thenReturn(Optional.empty());
        when(leaderboardManager.getObject(NAME)).thenReturn(Optional.of(leaderboard));

        bus = new RecordingMessageBus();
        // A disabled plugin runs a scheduled task inline, which is what makes the refresh observable here.
        sync = new LeaderboardSync(mock(Core.class), bus, leaderboardManager);
    }

    @AfterEach
    void tearDown() {
        Core.setCurrentRealm(originalRealm);
    }

    private BusMessage announcement(String origin, String name) {
        return BusMessage.builder()
                .topic(LeaderboardSync.TOPIC)
                .origin(origin)
                .payload(Map.of("name", name))
                .build();
    }

    @Test
    @DisplayName("a leaderboard that moved is named to the rest of the network")
    void aMovedLeaderboardIsAnnounced() {
        sync.changed(leaderboard);

        assertEquals(1, bus.published.size());
        assertEquals(NAME, bus.published.getFirst().getOrDefault("name", ""));
    }

    @Test
    @DisplayName("a busy leaderboard announces itself once rather than on every change")
    void announcementsAreSpacedOut() {
        sync.changed(leaderboard);
        sync.changed(leaderboard);
        sync.changed(leaderboard);

        assertEquals(1, bus.published.size());
    }

    @Test
    @DisplayName("nothing is announced when there is nowhere to announce it")
    void nothingIsAnnouncedWithoutABus() {
        bus.available = false;

        sync.changed(leaderboard);

        assertTrue(bus.published.isEmpty());
    }

    @Test
    @DisplayName("hearing that a standing moved elsewhere re-reads it")
    void anAnnouncementElsewhereRefreshesTheCopyHere() {
        bus.arrive(announcement(THERE, NAME));

        verify(leaderboard).forceUpdate();
    }

    @Test
    @DisplayName("this server does not re-read on its own announcement, having just made the change")
    void ourOwnAnnouncementIsIgnored() {
        bus.arrive(announcement(HERE, NAME));

        verify(leaderboard, never()).forceUpdate();
    }

    @Test
    @DisplayName("a leaderboard this server does not have is passed over")
    void anUnknownLeaderboardIsPassedOver() {
        bus.arrive(announcement(THERE, "SomethingElse"));

        verify(leaderboard, never()).forceUpdate();
    }
}
