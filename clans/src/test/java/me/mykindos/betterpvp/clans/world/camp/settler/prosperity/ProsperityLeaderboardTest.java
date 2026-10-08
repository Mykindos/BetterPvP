package me.mykindos.betterpvp.clans.world.camp.settler.prosperity;

import com.google.inject.Injector;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.stats.LeaderboardCategory;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProsperityLeaderboardTest {

    private final ClanManager clanManager = mock(ClanManager.class);
    private final FakeProsperityStore store = new FakeProsperityStore();
    private final Database database = mock(Database.class);
    private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    private ProsperityLeaderboard leaderboard;

    @BeforeEach
    void setUp() {
        final Clans clans = mock(Clans.class);
        when(clans.getInjector()).thenReturn(mock(Injector.class));
        when(clanManager.getClanByPlayer(any(UUID.class))).thenReturn(Optional.empty());
        try (MockedStatic<Executors> executors = mockStatic(Executors.class, Mockito.CALLS_REAL_METHODS)) {
            executors.when(Executors::newSingleThreadScheduledExecutor).thenReturn(scheduler);
            leaderboard = new ProsperityLeaderboard(clans, clanManager, store);
        }
    }

    @Test
    void ac10_itIsAClansLeaderboardOfTheTenMostProsperousCampsHighestFirst() {
        for (long clan = 1; clan <= 12; clan++) {
            store.save(clan, (int) clan * 10);
        }

        final Map<Long, Integer> top = leaderboard.fetchAll(SearchOptions.EMPTY, database);

        assertEquals(LeaderboardCategory.CLANS, leaderboard.getCategory());
        assertEquals(List.of(12L, 11L, 10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L), List.copyOf(top.keySet()));
        assertEquals(120, top.get(12L));
        assertTrue(leaderboard.getSorter(SearchOptions.EMPTY).compare(200, 100) < 0, "higher Prosperity sorts first");
    }

    @Test
    void ac11_aPlayersEntryIsTheirClansRecord() {
        final UUID player = UUID.randomUUID();
        final Clan clan = mock(Clan.class);
        when(clan.getId()).thenReturn(4L);
        when(clanManager.getClanByPlayer(player)).thenReturn(Optional.of(clan));
        store.save(4, 321);

        final LeaderboardEntry<Long, Integer> entry = leaderboard.fetchPlayerData(player, SearchOptions.EMPTY, database);

        assertEquals(4L, entry.getKey());
        assertEquals(321, entry.getValue());
    }

    @Test
    void ac11_noClanOrNoRecordMeansNoEntry() {
        final UUID clanless = UUID.randomUUID();
        assertNull(leaderboard.fetchPlayerData(clanless, SearchOptions.EMPTY, database));

        final UUID unrecorded = UUID.randomUUID();
        final Clan clan = mock(Clan.class);
        when(clan.getId()).thenReturn(8L);
        when(clanManager.getClanByPlayer(unrecorded)).thenReturn(Optional.of(clan));
        assertNull(leaderboard.fetchPlayerData(unrecorded, SearchOptions.EMPTY, database));
    }

    @Test
    void ac11_aClanWithNoRecordReadsZero() {
        store.save(2, 75);
        assertEquals(75, leaderboard.fetch(SearchOptions.EMPTY, database, 2L));
        assertEquals(0, leaderboard.fetch(SearchOptions.EMPTY, database, 9L));
    }

    @Test
    void ac12_itReadsTheStoreAgainEveryTenMinutes() {
        final ArgumentCaptor<Long> period = ArgumentCaptor.forClass(Long.class);
        final ArgumentCaptor<TimeUnit> unit = ArgumentCaptor.forClass(TimeUnit.class);
        verify(scheduler).scheduleAtFixedRate(any(Runnable.class), anyLong(), period.capture(), unit.capture());

        assertEquals(Duration.ofMinutes(10), Duration.of(period.getValue(), unit.getValue().toChronoUnit()));
    }
}
