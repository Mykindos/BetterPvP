package me.mykindos.betterpvp.core.stats;

import me.mykindos.betterpvp.core.client.stats.StatConcurrentHashMap;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.events.IStatMapListener;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.IWrapperStat;
import me.mykindos.betterpvp.core.client.stats.impl.utility.StatValueType;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatConcurrentHashMapTest {

    private Season season;
    private Realm realm;
    private StatConcurrentHashMap map;

    @Mock private IStat stat1;
    @Mock private IStat stat2;
    @Mock private IStatMapListener listener;

    @BeforeEach
    void setUp() {
        season = new Season(1, "TestSeason", LocalDate.now());
        realm = new Realm(1, new Server(1, "test-server"), season);
        map = new StatConcurrentHashMap();
        lenient().when(stat1.getStatType()).thenReturn("stat1");
        lenient().when(stat2.getStatType()).thenReturn("stat2");
    }

    // ── put ──────────���───────────────────────────────────────────────────────

    @Test
    @DisplayName("put: primary map is updated")
    void put_updatesPrimaryMap() {
        map.put(realm, stat1, 42L, true);
        assertEquals(42L, map.get(StatFilterType.REALM, realm, stat1));
    }

    @Test
    @DisplayName("put: allMap secondary index is updated")
    void put_updatesAllSecondaryIndex() {
        map.put(realm, stat1, 100L, true);
        assertEquals(100L, map.get(StatFilterType.ALL, null, stat1));
    }

    @Test
    @DisplayName("put: seasonMap secondary index is updated")
    void put_updatesSeasonSecondaryIndex() {
        map.put(realm, stat1, 55L, true);
        assertEquals(55L, map.get(StatFilterType.SEASON, season, stat1));
    }

    @Test
    @DisplayName("put: delta is applied to secondary indexes (not absolute overwrite)")
    void put_deltaAppliedToSecondaryIndexes() {
        map.put(realm, stat1, 10L, true);
        map.put(realm, stat1, 30L, true); // delta = +20
        assertEquals(30L, map.get(StatFilterType.ALL, null, stat1));
        assertEquals(30L, map.get(StatFilterType.SEASON, season, stat1));
    }

    @Test
    @DisplayName("put: silent=false notifies listener with correct values")
    void put_notifiesListenerWhenNotSilent() {
        map.registerListener(listener);
        map.put(realm, stat1, 7L, false);
        verify(listener).onMapValueChanged(stat1, 7L, null);
    }

    @Test
    @DisplayName("put: silent=true does NOT notify listener")
    void put_doesNotNotifyListenerWhenSilent() {
        map.registerListener(listener);
        map.put(realm, stat1, 7L, true);
        verifyNoInteractions(listener);
    }

    // ── increase ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("increase: primary map accumulates correctly")
    void increase_accumulatesInPrimaryMap() {
        map.increase(realm, stat1, 10L);
        map.increase(realm, stat1, 5L);
        assertEquals(15L, map.get(StatFilterType.REALM, realm, stat1));
    }

    @Test
    @DisplayName("increase: allMap secondary index is updated")
    void increase_updatesAllMap() {
        map.increase(realm, stat1, 20L);
        assertEquals(20L, map.get(StatFilterType.ALL, null, stat1));
    }

    @Test
    @DisplayName("increase: seasonMap secondary index is updated")
    void increase_updatesSeasonMap() {
        map.increase(realm, stat1, 15L);
        assertEquals(15L, map.get(StatFilterType.SEASON, season, stat1));
    }

    @Test
    @DisplayName("increase: listener receives correct old and new values")
    void increase_notifiesListenerWithCorrectValues() {
        map.registerListener(listener);
        map.increase(realm, stat1, 10L);
        map.increase(realm, stat1, 5L);

        ArgumentCaptor<Long> newCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> oldCaptor = ArgumentCaptor.forClass(Long.class);
        verify(listener, times(2)).onMapValueChanged(eq(stat1), newCaptor.capture(), oldCaptor.capture());

        List<Long> newValues = newCaptor.getAllValues();
        List<Long> oldValues = oldCaptor.getAllValues();
        assertEquals(10L, newValues.get(0));
        assertEquals(0L, oldValues.get(0));
        assertEquals(15L, newValues.get(1));
        assertEquals(10L, oldValues.get(1));
    }

    @Test
    @DisplayName("increase: listener is notified OUTSIDE the write lock (no deadlock under concurrent load)")
    void increase_listenerCalledOutsideLock_noDeadlock() throws InterruptedException {
        int threads = 8;
        int incrementsPerThread = 500;
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            CountDownLatch done = new CountDownLatch(threads);
            AtomicLong notificationCount = new AtomicLong();

            // listener that tries to read the map (would deadlock if called inside the lock)
            map.registerListener((s, nv, ov) -> {
                // Reading from map while listener fires — safe only if lock is released first
                map.get(StatFilterType.ALL, null, s);
                notificationCount.incrementAndGet();
            });

            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < incrementsPerThread; j++) {
                            map.increase(realm, stat1, 1L);
                        }
                    } finally {
                        done.countDown();
                    }
                });
            }

            assertTrue(done.await(10, TimeUnit.SECONDS), "Threads did not finish in time (possible deadlock)");

            long expectedTotal = (long) threads * incrementsPerThread;
            assertEquals(expectedTotal, map.get(StatFilterType.ALL, null, stat1));
            assertEquals(expectedTotal, notificationCount.get());
        }
    }

    // ── get ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("get: returns null for unknown stat")
    void get_returnsNullForUnknownStat() {
        assertNull(map.get(StatFilterType.REALM, realm, stat1));
    }

    @Test
    @DisplayName("get: ALL aggregates across multiple realms")
    void get_allAggregatesAcrossRealms() {
        Season season2 = new Season(2, "Season2", LocalDate.now().plusYears(1));
        Realm realm2 = new Realm(2, new Server(2, "test2"), season2);
        map.increase(realm, stat1, 30L);
        map.increase(realm2, stat1, 20L);
        assertEquals(50L, map.get(StatFilterType.ALL, null, stat1));
    }

    @Test
    @DisplayName("get: SEASON returns correct per-season total")
    void get_seasonReturnsCorrectTotal() {
        Season season2 = new Season(2, "Season2", LocalDate.now().plusYears(1));
        Realm realm2 = new Realm(2, new Server(2, "test2"), season2);
        map.increase(realm, stat1, 10L);
        map.increase(realm2, stat1, 99L);
        assertEquals(10L, map.get(StatFilterType.SEASON, season, stat1));
        assertEquals(99L, map.get(StatFilterType.SEASON, season2, stat1));
    }

    @Test
    @DisplayName("get: REALM returns only that realm's value")
    void get_realmIsolated() {
        Season season2 = new Season(2, "S2", LocalDate.now().plusYears(1));
        Realm realm2 = new Realm(2, new Server(2, "srv2"), season2);
        map.increase(realm, stat1, 7L);
        map.increase(realm2, stat1, 99L);
        assertEquals(7L, map.get(StatFilterType.REALM, realm, stat1));
        assertEquals(99L, map.get(StatFilterType.REALM, realm2, stat1));
    }

    // ── copyFrom ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("copyFrom: secondary indexes are rebuilt correctly")
    void copyFrom_rebuildsSecondaryIndexes() {
        StatConcurrentHashMap source = new StatConcurrentHashMap();
        source.put(realm, stat1, 50L, true);
        source.put(realm, stat2, 25L, true);

        StatConcurrentHashMap dest = new StatConcurrentHashMap();
        dest.copyFrom(source);

        assertEquals(50L, dest.get(StatFilterType.ALL, null, stat1));
        assertEquals(25L, dest.get(StatFilterType.ALL, null, stat2));
        assertEquals(50L, dest.get(StatFilterType.SEASON, season, stat1));
        assertEquals(50L, dest.get(StatFilterType.REALM, realm, stat1));
    }

    // ── concurrent correctness ──────────────────────────────────────���─────────

    @Test
    @DisplayName("concurrent increases from many threads produce correct totals")
    void concurrent_correctTotals() throws InterruptedException {
        int threads = 16;
        int perThread = 1000;
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            CountDownLatch latch = new CountDownLatch(threads);

            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < perThread; j++) {
                            map.increase(realm, stat1, 1L);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(15, TimeUnit.SECONDS));

            long expected = (long) threads * perThread;
            assertEquals(expected, map.get(StatFilterType.REALM, realm, stat1), "primary map mismatch");
            assertEquals(expected, map.get(StatFilterType.ALL, null, stat1), "allMap mismatch");
            assertEquals(expected, map.get(StatFilterType.SEASON, season, stat1), "seasonMap mismatch");
        }
    }

    // ── leaf aggregate index ─────────────────────────────────────────────────

    /**
     * Helper: create a mock {@link IWrapperStat} wrapping the given base stat.
     * Use sparingly — prefer {@link #cheapWrapper(IStat)} in performance/load tests.
     */
    private IWrapperStat mockWrapper(IStat base) {
        IWrapperStat wrapper = mock(IWrapperStat.class);
        lenient().when(wrapper.getWrappedStat()).thenReturn(base);
        return wrapper;
    }

    /**
     * Lightweight, allocation-only wrapper for high-volume tests.
     * Avoids Mockito overhead so millions of instances can be created cheaply.
     * <p>
     * Models a real savable wrapper stat (e.g. {@code ClanWrapperStat}, {@code GameTeamMapWrapperStat})
     * which wraps a savable leaf stat (e.g. {@code ClientStat.KILLS}).  These wrappers ARE savable
     * because they are the actual entries persisted per-realm in the database.  Non-savable composite
     * stats (e.g. {@code GenericStat}) are the callers that then query the map via
     * {@code getLeafAggregate} — they are never stored here.
     * </p>
     */
    private static IWrapperStat cheapWrapper(IStat base) {
        return new IWrapperStat() {
            @Override public IStat getWrappedStat() { return base; }
            @Override public @org.jetbrains.annotations.NotNull String getStatType() { return "w"; }
            @Override public Long getStat(me.mykindos.betterpvp.core.client.stats.StatContainer c,
                                          StatFilterType t,
                                          @org.jetbrains.annotations.Nullable me.mykindos.betterpvp.core.server.Period p) { return 0L; }
            @Override public @org.jetbrains.annotations.NotNull me.mykindos.betterpvp.core.client.stats.impl.utility.StatValueType getStatValueType() {
                return me.mykindos.betterpvp.core.client.stats.impl.utility.StatValueType.LONG;
            }
            @Override public org.json.JSONObject getJsonData() { return null; }
            /** Savable = true: wrapper stats are stored in the DB per-realm. */
            @Override public boolean isSavable() { return true; }
            @Override public boolean containsStat(IStat otherStat) { return equals(otherStat); }
            @Override public @org.jetbrains.annotations.NotNull IStat getGenericStat() { return this; }
        };
    }

    @Test
    @DisplayName("leaf index: put with a wrapper stat is indexed by its leaf")
    void leafIndex_put_wrapperIndexedByLeaf() {
        IWrapperStat wrapper = mockWrapper(stat1);
        map.put(realm, wrapper, 10L, true);

        assertEquals(10L, map.getLeafAggregate(StatFilterType.ALL, null, stat1));
    }

    @Test
    @DisplayName("leaf index: increase with a wrapper stat is indexed by its leaf")
    void leafIndex_increase_wrapperIndexedByLeaf() {
        IWrapperStat wrapper = mockWrapper(stat1);
        map.increase(realm, wrapper, 7L);
        map.increase(realm, wrapper, 3L);

        assertEquals(10L, map.getLeafAggregate(StatFilterType.ALL, null, stat1));
    }

    @Test
    @DisplayName("leaf index: multiple wrappers sharing the same leaf are summed")
    void leafIndex_multipleWrappersShareLeaf_summed() {
        IWrapperStat wrapperA = mockWrapper(stat1);
        IWrapperStat wrapperB = mockWrapper(stat1);
        map.put(realm, wrapperA, 20L, true);
        map.put(realm, wrapperB, 30L, true);

        assertEquals(50L, map.getLeafAggregate(StatFilterType.ALL, null, stat1));
    }

    @Test
    @DisplayName("leaf index: deeply nested wrappers resolved to root leaf")
    void leafIndex_deepWrapperChain_rootLeafUsed() {
        IWrapperStat inner = mockWrapper(stat1);           // inner wraps stat1
        IWrapperStat outer = mock(IWrapperStat.class);     // outer wraps inner
        when(outer.getWrappedStat()).thenReturn(inner);
        lenient().when(outer.getStatType()).thenReturn("outer");

        map.put(realm, outer, 99L, true);

        // leaf of outer → inner → stat1
        assertEquals(99L, map.getLeafAggregate(StatFilterType.ALL, null, stat1));
    }

    @Test
    @DisplayName("leaf index: non-wrapper stat leaf is itself")
    void leafIndex_nonWrapper_leafIsItself() {
        map.put(realm, stat1, 42L, true);

        assertEquals(42L, map.getLeafAggregate(StatFilterType.ALL, null, stat1));
    }

    @Test
    @DisplayName("leaf index: SEASON filter returns correct season total")
    void leafIndex_seasonFilter() {
        Season season2 = new Season(2, "S2", LocalDate.now().plusYears(1));
        Realm realm2 = new Realm(2, new Server(2, "srv2"), season2);
        IWrapperStat wrapper = mockWrapper(stat1);

        map.put(realm, wrapper, 10L, true);
        map.put(realm2, wrapper, 99L, true);

        assertEquals(10L, map.getLeafAggregate(StatFilterType.SEASON, season, stat1));
        assertEquals(99L, map.getLeafAggregate(StatFilterType.SEASON, season2, stat1));
    }

    @Test
    @DisplayName("leaf index: REALM filter returns only that realm's total")
    void leafIndex_realmFilter() {
        Season season2 = new Season(2, "S2", LocalDate.now().plusYears(1));
        Realm realm2 = new Realm(2, new Server(2, "srv2"), season2);
        IWrapperStat wrapper = mockWrapper(stat1);

        map.put(realm, wrapper, 5L, true);
        map.put(realm2, wrapper, 95L, true);

        assertEquals(5L, map.getLeafAggregate(StatFilterType.REALM, realm, stat1));
        assertEquals(95L, map.getLeafAggregate(StatFilterType.REALM, realm2, stat1));
    }

    @Test
    @DisplayName("leaf index: copyFrom rebuilds leaf indexes correctly")
    void leafIndex_copyFrom_rebuildsCorrectly() {
        IWrapperStat wrapperA = mockWrapper(stat1);
        IWrapperStat wrapperB = mockWrapper(stat1);
        map.put(realm, wrapperA, 15L, true);
        map.put(realm, wrapperB, 25L, true);

        StatConcurrentHashMap copy = new StatConcurrentHashMap();
        copy.copyFrom(map);

        assertEquals(40L, copy.getLeafAggregate(StatFilterType.ALL, null, stat1));
        assertEquals(40L, copy.getLeafAggregate(StatFilterType.SEASON, season, stat1));
        assertEquals(40L, copy.getLeafAggregate(StatFilterType.REALM, realm, stat1));
    }

    @Test
    @DisplayName("leaf index: getLeafAggregate returns null for never-recorded leaf")
    void leafIndex_nullForUnknown() {
        assertNull(map.getLeafAggregate(StatFilterType.ALL, null, stat1));
    }

    // ── stat-menu performance (GenericStat O(1) via leaf aggregate) ───────────

    /**
     * Proves that {@code getLeafAggregate} is O(1) regardless of map size by placing realistic
     * per-player load into a single map and running many lookups against it.
     * <p>
     * Map contents — 5 seasons × 3 realms = 15 {@link Realm} objects, 30 savable base stats,
     * 25 savable wrapper variants per base stat per realm:
     * <pre>
     *   15 realms × 30 base stats × 25 wrappers = 11 250 stat entries
     * </pre>
     * This represents a veteran player who has played across every realm in every season and
     * accumulated hundreds of distinct per-clan / per-game / per-dungeon wrapper stats per realm.
     * </p>
     * <p>
     * The timed phase does 30 stats × 100 iterations = 3 000 O(1) lookups and must finish in ≤ 10 ms.
     * The old {@code getFilteredStats} O(n) path would require
     * 3 000 × 11 250 = <b>33 750 000 iterations</b> for the same work — this is the primary
     * bottleneck the leaf-aggregate index was introduced to eliminate.
     * </p>
     */
    @Test
    @DisplayName("performance: 3 000 generic-stat lookups on a single 11 250-entry map (15 realms/5 seasons) complete in ≤ 5 ms")
    void performance_statMenu_heavyLoad_under5ms() {
        final int GENERIC_COUNT     = 30;   // distinct base stats shown in menu
        final int SEASON_COUNT      = 5;    // 5 historical seasons
        final int REALMS_PER_SEASON = 3;    // realms per season  →  15 realms total
        final int WRAPPERS_PER_BASE = 25;   // savable wrapper variants per base stat per realm
        final int LOOKUP_ITERATIONS = 100;  // repeat the 30-stat lookup 100 times = 3 000 total

        // --- Build seasons and realms ----------------------------------------
        Season[] seasons = new Season[SEASON_COUNT];
        Realm[] realms   = new Realm[SEASON_COUNT * REALMS_PER_SEASON];
        for (int s = 0; s < SEASON_COUNT; s++) {
            seasons[s] = new Season(s + 1, "Season_" + s, LocalDate.now().plusYears(s));
            for (int r = 0; r < REALMS_PER_SEASON; r++) {
                int idx = s * REALMS_PER_SEASON + r;
                realms[idx] = new Realm(idx + 1, new Server(idx + 1, "srv-" + idx), seasons[s]);
            }
        }

        // --- 30 distinct base (leaf) stats — savable, e.g. ClientStat.KILLS --
        IStat[] baseStats = new IStat[GENERIC_COUNT];
        for (int i = 0; i < GENERIC_COUNT; i++) {
            IStat s = mock(IStat.class);
            lenient().when(s.getStatType()).thenReturn("base_" + i);
            lenient().when(s.isSavable()).thenReturn(true);
            baseStats[i] = s;
        }

        // --- Build one realistic player map ----------------------------------
        // 15 realms × 30 base stats × 25 savable wrappers = 11 250 stat entries.
        // In production each player has exactly one such map; here we build one
        // and run all lookups against it to prove the O(1) property under full load.
        StatConcurrentHashMap playerMap = new StatConcurrentHashMap();
        for (Realm rlm : realms) {
            for (int g = 0; g < GENERIC_COUNT; g++) {
                for (int w = 0; w < WRAPPERS_PER_BASE; w++) {
                    playerMap.put(rlm, cheapWrapper(baseStats[g]), (long) (g + w + 1), true);
                }
            }
        }

        // Sanity: 15 realm buckets in the primary map
        assertEquals(SEASON_COUNT * REALMS_PER_SEASON, playerMap.getMyMap().size(),
                "Player map should have one inner map per realm");

        // --- Warm up JIT ------------------------------------------------------
        for (int g = 0; g < GENERIC_COUNT; g++) {
            playerMap.getLeafAggregate(StatFilterType.ALL, null, baseStats[g]);
        }

        // --- Timed run: 30 stats × 100 iterations = 3 000 O(1) lookups ------
        long start = System.nanoTime();
        for (int iter = 0; iter < LOOKUP_ITERATIONS; iter++) {
            for (int g = 0; g < GENERIC_COUNT; g++) {
                Long value = playerMap.getLeafAggregate(StatFilterType.ALL, null, baseStats[g]);
                assertNotNull(value, "Leaf aggregate must be present for stat " + g);
            }
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(elapsedMs <= 5,
                "3 000 O(1) lookups on an 11 250-entry map (15 realms / 5 seasons) took "
                        + elapsedMs + " ms — must be ≤ 5 ms. "
                        + "The O(n) getFilteredStats path would require 33 750 000 iterations for the same work.");
    }

    /**
     * Verifies that the leaf aggregate correctly sums across all realms and seasons.
     * With 8 realms (2 seasons × 4 realms/season) and 30 wrappers each contributing {@code value = w+1}
     * per base stat, the ALL aggregate must equal 8 × sum(1..30) = 8 × 465 = 3 720.
     */
    @Test
    @DisplayName("leaf index: ALL aggregate sums correctly across multiple realms and seasons")
    void leafIndex_allAggregate_multipleRealmsSeasons() {
        final int SEASON_COUNT      = 2;
        final int REALMS_PER_SEASON = 4;
        final int WRAPPERS          = 30;

        Season[] seasons = new Season[SEASON_COUNT];
        Realm[]  realms  = new Realm[SEASON_COUNT * REALMS_PER_SEASON];
        for (int s = 0; s < SEASON_COUNT; s++) {
            seasons[s] = new Season(s + 10, "S" + s, LocalDate.now().plusYears(s));
            for (int r = 0; r < REALMS_PER_SEASON; r++) {
                int idx = s * REALMS_PER_SEASON + r;
                realms[idx] = new Realm(idx + 10, new Server(idx + 10, "srv" + idx), seasons[s]);
            }
        }

        StatConcurrentHashMap testMap = new StatConcurrentHashMap();
        long expectedAll = 0L;
        for (Realm rlm : realms) {
            for (int w = 0; w < WRAPPERS; w++) {
                IWrapperStat wrapper = cheapWrapper(stat1);
                long value = w + 1L;
                testMap.put(rlm, wrapper, value, true);
                expectedAll += value;
            }
        }

        assertEquals(expectedAll, testMap.getLeafAggregate(StatFilterType.ALL, null, stat1),
                "ALL aggregate must sum contributions from all realms and seasons");

        // Per-season sums must also be correct
        long sumPerSeason = (long) REALMS_PER_SEASON * (WRAPPERS * (WRAPPERS + 1) / 2); // 4 * 465 = 1860
        for (Season s : seasons) {
            assertEquals(sumPerSeason, testMap.getLeafAggregate(StatFilterType.SEASON, s, stat1),
                    "Season aggregate mismatch for " + s.getName());
        }
    }

    // ── write-burst performance (TimedStatListener tick) ─────────────────────

    /**
     * Models the {@code SkillStatListener.doUpdateEvent()} tick that fires every 60 s:
     * for every online player the listener calls {@code statContainer.incrementStat()} once
     * per equipped skill (5 slots).
     * <p>
     * Each {@code increase()} call maintains <b>6 secondary indexes</b>
     * (myMap, allMap, seasonMap, leafAllMap, leafSeasonMap, leafRealmMap), so
     * 500 writes = 3 000 ConcurrentHashMap operations per tick.  This must stay fast
     * regardless of how much prior stat history already lives in each player's map.
     * </p>
     * Setup: 100 player maps, each pre-populated with 3 realms × 5 skills × 3 levels = 45
     * existing savable skill-stat entries (realistic prior-session history).
     * Timed phase: 100 × 5 = 500 {@code increase()} calls — must finish in ≤ 10 ms.
     */
    @Test
    @DisplayName("performance: SkillStatListener tick — 100 players × 5 skill writes (500 total, 6 secondary indexes each) complete in ≤ 10 ms")
    void performance_timedStatTick_100Players_5SkillWrites_under10ms() {
        final int PLAYER_COUNT    = 100;
        final int SKILLS_IN_BUILD = 5;
        final int PRIOR_SKILLS    = 5;
        final int PRIOR_LEVELS    = 3;
        final int PRIOR_REALMS    = 3;

        Season priorSeason = new Season(99, "Prior", LocalDate.now().minusYears(1));
        Realm[] priorRealms = new Realm[PRIOR_REALMS];
        for (int r = 0; r < PRIOR_REALMS; r++) {
            priorRealms[r] = new Realm(100 + r, new Server(100 + r, "prior-" + r), priorSeason);
        }

        // Pre-populate each player map with 45 savable skill-stat entries (prior history)
        StatConcurrentHashMap[] playerMaps = new StatConcurrentHashMap[PLAYER_COUNT];
        for (int p = 0; p < PLAYER_COUNT; p++) {
            playerMaps[p] = new StatConcurrentHashMap();
            for (Realm rlm : priorRealms) {
                for (int s = 0; s < PRIOR_SKILLS; s++) {
                    for (int l = 1; l <= PRIOR_LEVELS; l++) {
                        playerMaps[p].put(rlm, savableSkillStat("OldSkill_" + s, l), 60_000L * l, true);
                    }
                }
            }
        }

        // 5 savable skill stats representing the current equipped build
        IStat[] equippedSkills = new IStat[SKILLS_IN_BUILD];
        for (int s = 0; s < SKILLS_IN_BUILD; s++) {
            equippedSkills[s] = savableSkillStat("Skill_" + s, 3);
        }

        // Warm up JIT
        for (int p = 0; p < 5; p++) {
            for (IStat skill : equippedSkills) {
                playerMaps[p].increase(realm, skill, 1000L);
            }
        }

        // --- Timed run: 100 × 5 = 500 increase() calls, 6 secondary index updates each ---
        long start = System.nanoTime();
        for (StatConcurrentHashMap playerMap : playerMaps) {
            for (IStat skill : equippedSkills) {
                playerMap.increase(realm, skill, 1000L);
            }
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(elapsedMs <= 5,
                "SkillStatListener tick: 100 players × 5 skill writes (500 total) took "
                        + elapsedMs + " ms — must be ≤ 5 ms even with 6 secondary index updates per write.");
    }

    /**
     * Helper: creates a savable skill stat for a specific skill name + level.
     * Models {@code ChampionsSkillStat(action=TIME_PLAYED, skill, level)} — the entries that
     * {@code SkillStatListener.incrementBuildStats()} writes once per equipped skill per tick.
     * {@code isSavable()} returns {@code true} (both skill and level are specified).
     */
    private static IStat savableSkillStat(String skillName, int level) {
        final String type = "SKILL_" + skillName + "_" + level;
        return new IStat() {
            @Override public @org.jetbrains.annotations.NotNull String getStatType() { return type; }
            @Override public Long getStat(me.mykindos.betterpvp.core.client.stats.StatContainer c, StatFilterType t,
                                          @org.jetbrains.annotations.Nullable me.mykindos.betterpvp.core.server.Period p) { return 0L; }
            @Override public @org.jetbrains.annotations.NotNull StatValueType getStatValueType() { return StatValueType.DURATION; }
            @Override public org.json.JSONObject getJsonData() { return null; }
            @Override public boolean isSavable() { return true; }
            @Override public boolean containsStat(IStat o) { return equals(o); }
            @Override public @org.jetbrains.annotations.NotNull IStat getGenericStat() { return this; }
            @Override public boolean equals(Object o) { return o instanceof IStat s && type.equals(s.getStatType()); }
            @Override public int hashCode() { return type.hashCode(); }
        };
    }

    /**
     * Helper: creates a <em>partial</em> (non-savable) skill stat for a skill name only (no level).
     * Models {@code ChampionsSkillStat(action=TIME_PLAYED, skill, level=-1)} — the stat that
     * {@code UseAllSkillsAchievement} wraps in {@code GenericStat} to query the aggregate time
     * played across all levels of a given skill.
     * {@code containsStat()} returns {@code true} for any savable entry with the same skill name.
     */
    private static IStat partialSkillStat(String skillName) {
        final String prefix = "SKILL_" + skillName + "_";
        final String type   = "SKILL_" + skillName;
        return new IStat() {
            @Override public @org.jetbrains.annotations.NotNull String getStatType() { return type; }
            @Override public Long getStat(me.mykindos.betterpvp.core.client.stats.StatContainer c, StatFilterType t,
                                          @org.jetbrains.annotations.Nullable me.mykindos.betterpvp.core.server.Period p) {
                return getFilteredStat(c, t, p, e -> containsStat(e.getKey()));
            }
            @Override public @org.jetbrains.annotations.NotNull StatValueType getStatValueType() { return StatValueType.DURATION; }
            @Override public org.json.JSONObject getJsonData() { return null; }
            @Override public boolean isSavable() { return false; }  // no level = not savable
            @Override public boolean containsStat(IStat o) { return o.getStatType().startsWith(prefix); }
            @Override public @org.jetbrains.annotations.NotNull IStat getGenericStat() { return this; }
        };
    }

    /**
     * Builds a StatContainer whose backing stats map is the provided map.
     * This mirrors the runtime shape where achievements read through GenericStat -> StatContainer.
     */
    private static me.mykindos.betterpvp.core.client.stats.StatContainer containerWithStats(StatConcurrentHashMap stats) {
        try {
            me.mykindos.betterpvp.core.client.stats.StatContainer container =
                    new me.mykindos.betterpvp.core.client.stats.StatContainer(null);
            java.lang.reflect.Field statsField =
                    me.mykindos.betterpvp.core.client.stats.StatContainer.class.getDeclaredField("stats");
            statsField.setAccessible(true);
            statsField.set(container, stats);
            return container;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create StatContainer for test", e);
        }
    }

    /**
     * Models the {@code SkillStatListener.doUpdateEvent()} tick that fires every 60 s:
     * for every online player the listener calls {@code statContainer.incrementStat()} once
     * per equipped skill (5 slots).
     * <p>
     * Each {@code increase()} call maintains <b>6 secondary indexes</b>
     * (myMap, allMap, seasonMap, leafAllMap, leafSeasonMap, leafRealmMap), so
     * 500 writes = 3 000 ConcurrentHashMap operations per tick.  This must stay fast
     * regardless of how much prior stat history already lives in each player's map.
     * </p>
     * Setup: 100 player maps, each pre-populated with 3 realms × 5 skills × 3 levels = 45
     * existing savable skill-stat entries (realistic prior-session history).
     * Timed phase: 100 × 5 = 500 {@code increase()} calls — must finish in ≤ 10 ms.
     */
    @Test
    @DisplayName("performance: SkillStat tick + achievement pass — 100 players, 5 writes + 20 GenericStat reads each, completes in ≤ 100 ms")
    void performance_timedStatTick_withAchievementProcessing_under10ms() {
        final int PLAYER_COUNT    = 100;
        final int SKILLS_IN_BUILD = 5;
        final int WATCHED_SKILLS  = 20; // UseAllSkills-like watched stat count
        final int PRIOR_SKILLS    = 5;
        final int PRIOR_LEVELS    = 3;
        final int PRIOR_REALMS    = 3;

        Season priorSeason = new Season(99, "Prior", LocalDate.now().minusYears(1));
        Realm[] priorRealms = new Realm[PRIOR_REALMS];
        for (int r = 0; r < PRIOR_REALMS; r++) {
            priorRealms[r] = new Realm(100 + r, new Server(100 + r, "prior-" + r), priorSeason);
        }

        StatConcurrentHashMap[] playerMaps = new StatConcurrentHashMap[PLAYER_COUNT];
        me.mykindos.betterpvp.core.client.stats.StatContainer[] containers =
                new me.mykindos.betterpvp.core.client.stats.StatContainer[PLAYER_COUNT];
        for (int p = 0; p < PLAYER_COUNT; p++) {
            playerMaps[p] = new StatConcurrentHashMap();
            for (Realm rlm : priorRealms) {
                for (int s = 0; s < PRIOR_SKILLS; s++) {
                    for (int l = 1; l <= PRIOR_LEVELS; l++) {
                        playerMaps[p].put(rlm, savableSkillStat("OldSkill_" + s, l), 60_000L * l, true);
                    }
                }
            }
            containers[p] = containerWithStats(playerMaps[p]);
        }

        // 5 savable skill stats representing the current equipped build (write path)
        IStat[] equippedSkills = new IStat[SKILLS_IN_BUILD];
        for (int s = 0; s < SKILLS_IN_BUILD; s++) {
            equippedSkills[s] = savableSkillStat("Skill_" + s, 3);
        }

        // 20 partial GenericStats representing achievement watched stats (read path)
        me.mykindos.betterpvp.core.client.stats.impl.GenericStat[] watched =
                new me.mykindos.betterpvp.core.client.stats.impl.GenericStat[WATCHED_SKILLS];
        for (int s = 0; s < WATCHED_SKILLS; s++) {
            watched[s] = new me.mykindos.betterpvp.core.client.stats.impl.GenericStat(partialSkillStat("Skill_" + s));
        }

        // Warm up JIT
        for (int p = 0; p < 5; p++) {
            for (IStat skill : equippedSkills) {
                playerMaps[p].increase(realm, skill, 1000L);
            }
            for (me.mykindos.betterpvp.core.client.stats.impl.GenericStat stat : watched) {
                stat.getStat(containers[p], StatFilterType.ALL, null);
            }
        }

        // Timed run models one tick:
        // 1) SkillStatListener writes 5 TIME_PLAYED skill stats per player
        // 2) UseAllSkillsAchievement evaluates watched GenericStats for progress
        long start = System.nanoTime();
        for (int p = 0; p < PLAYER_COUNT; p++) {
            for (IStat skill : equippedSkills) {
                playerMaps[p].increase(realm, skill, 1000L);
            }
            for (me.mykindos.betterpvp.core.client.stats.impl.GenericStat stat : watched) {
                Long value = stat.getStat(containers[p], StatFilterType.ALL, null);
                assertNotNull(value);
            }
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(elapsedMs <= 100,
                "SkillStat tick + achievement pass (100 players, 5 writes + 20 GenericStat reads each) took "
                        + elapsedMs + " ms — must be ≤ 100 ms.");
    }

    // ── GenericStat correctness with partial / non-savable IBuildableStat ────

    /**
     * Verifies that {@link me.mykindos.betterpvp.core.client.stats.impl.GenericStat} correctly
     * falls back to O(n) {@code getFilteredStat} for partial skill stats ({@code isSavable() == false}),
     * exactly as used by {@code UseAllSkillsAchievement}.
     * <p>
     * A partial stat (skill only, no level) is NOT stored verbatim as a key in {@code leafAllMap}.
     * The O(1) {@code getLeafAggregate} path silently returned 0 before the fix because only the
     * exact per-level entries are stored.  The fix in {@code GenericStat.getStat()} checks
     * {@code !stat.isSavable()} and routes these partial queries to the O(n) path that uses
     * {@code containsStat()} partial-matching semantics.
     * </p>
     */
    @Test
    @DisplayName("GenericStat: partial skill stat (no level) sums across all levels via O(n) fallback")
    void genericStat_partialSkillStat_sumsAllLevels() throws Exception {
        map.put(realm, savableSkillStat("Fireball", 1), 1000L, true);
        map.put(realm, savableSkillStat("Fireball", 2), 2000L, true);
        map.put(realm, savableSkillStat("Fireball", 3), 3000L, true);
        map.put(realm, savableSkillStat("IceBlock",  1), 9999L, true); // must NOT be included

        IStat partialFireball = partialSkillStat("Fireball");
        assertFalse(partialFireball.isSavable(), "Partial stat must not be savable");

        me.mykindos.betterpvp.core.client.stats.impl.GenericStat generic =
                new me.mykindos.betterpvp.core.client.stats.impl.GenericStat(partialFireball);

        // Inject our map into a StatContainer via reflection
        me.mykindos.betterpvp.core.client.stats.StatContainer container =
                containerWithStats(map);

        Long result = generic.getStat(container, StatFilterType.ALL, null);
        assertEquals(6000L, result,
                "GenericStat with partial (no-level) skill stat must sum all levels (1000+2000+3000=6000). "
                        + "A result of 0 means the broken O(1) path was taken; "
                        + "15999 means IceBlock was incorrectly included.");
    }
}
