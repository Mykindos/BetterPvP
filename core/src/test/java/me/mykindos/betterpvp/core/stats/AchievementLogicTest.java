package me.mykindos.betterpvp.core.stats;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletion;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementCompletionsConcurrentHashMap;
import me.mykindos.betterpvp.core.client.achievements.repository.AchievementManager;
import me.mykindos.betterpvp.core.client.achievements.types.Achievement;
import me.mykindos.betterpvp.core.client.stats.StatConcurrentHashMap;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.events.StatPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the core {@link Achievement} logic: stat matching, delta reconstruction,
 * threshold notifications, completion detection, and periodic forceCheck.
 *
 * <p>Uses {@link MockedStatic} to intercept {@link JavaPlugin#getPlugin(Class)} before
 * {@link Achievement} is first loaded, preventing the static initializer from touching
 * a real Bukkit server.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AchievementLogicTest {

    // ── static mocks, opened before Achievement class is loaded ───────────────
    private MockedStatic<JavaPlugin> mockedJavaPlugin;
    private MockedStatic<Bukkit> mockedBukkit;

    private AchievementManager mockAchievementManager;
    private Core mockCore;

    // ── test domain objects ──────────────────────────────────────────────────
    private Season season;
    private Realm realm;
    private StatContainer container;
    private StatConcurrentHashMap statsMap;
    private AchievementCompletionsConcurrentHashMap completions;
    private IStat watchedStat;

    @BeforeAll
    void openStaticMocks() {
        // Must happen BEFORE Achievement class is loaded (i.e. before any reference to TestAchievement)
        mockCore = mock(Core.class, Mockito.RETURNS_DEEP_STUBS);
        mockAchievementManager = mock(AchievementManager.class);
        when(mockCore.getInjector().getInstance(AchievementManager.class))
                .thenReturn(mockAchievementManager);
        // isEnabled() → false by default, so UtilServer.runTask will execute the runnable synchronously
        when(mockCore.isEnabled()).thenReturn(false);

        mockedJavaPlugin = Mockito.mockStatic(JavaPlugin.class);
        mockedJavaPlugin.when(() -> JavaPlugin.getPlugin(Core.class)).thenReturn(mockCore);

        mockedBukkit = Mockito.mockStatic(Bukkit.class);
        // Bukkit.getPlayer() returns null when called from tests — acceptable since we only verify calls
    }

    @AfterAll
    void closeStaticMocks() {
        mockedJavaPlugin.close();
        mockedBukkit.close();
    }

    @BeforeEach
    void setUp() {
        season = new Season(1, "TestSeason", LocalDate.now());
        realm = new Realm(1, new Server(1, "srv"), season);

        watchedStat = mock(IStat.class);
        when(watchedStat.isSavable()).thenReturn(true);
        when(watchedStat.containsStat(watchedStat)).thenReturn(true);
        lenient().when(watchedStat.getStatType()).thenReturn("test_stat");

        statsMap = new StatConcurrentHashMap();
        completions = mock(AchievementCompletionsConcurrentHashMap.class);

        container = mock(StatContainer.class);
        when(container.getStats()).thenReturn(statsMap);
        when(container.getAchievementCompletions()).thenReturn(completions);
        when(container.getUniqueId()).thenReturn(UUID.randomUUID());
        when(container.getProperty(any(), any(), any())).thenAnswer(inv ->
                statsMap.get(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)) == null
                        ? Long.valueOf(0L) : statsMap.get(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)));

        // no pre-existing completion by default
        when(completions.getCompletion(any(), any())).thenReturn(Optional.empty());

        // reset achievementManager mock
        reset(mockAchievementManager);
        when(mockAchievementManager.saveCompletion(any(), any(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Concrete achievement: complete when watchedStat >= goal. */
    private TestAchievement makeAchievement(long goal) {
        return new TestAchievement(
                "Test Achievement",
                new NamespacedKey("test", "test_achievement"),
                StatFilterType.ALL,
                goal,
                watchedStat
        );
    }

    private StatPropertyUpdateEvent makeEvent(IStat stat, Long newVal, Long oldVal) {
        return new StatPropertyUpdateEvent(container, stat, newVal, oldVal);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // onPropertyChangeListener — stat matching
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("onPropertyChangeListener: relevant stat is recognized and onChangeValue is called")
    void listener_relevantStatTriggersOnChangeValue() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(true);

        statsMap.put(realm, watchedStat, 50L, true); // seed via a real realm; reads still use StatFilterType.ALL aggregation

        StatPropertyUpdateEvent event = makeEvent(watchedStat, 50L, 0L);
        achievement.onPropertyChangeListener(event);

        assertTrue(achievement.onChangeValueCalled, "onChangeValue should have been called");
    }

    @Test
    @Order(11)
    @DisplayName("onPropertyChangeListener: unrelated stat is ignored")
    void listener_unrelatedStatIsIgnored() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(true);

        IStat unrelated = mock(IStat.class);
        when(unrelated.containsStat(any())).thenReturn(false);
        when(watchedStat.containsStat(unrelated)).thenReturn(false);

        StatPropertyUpdateEvent event = makeEvent(unrelated, 50L, 0L);
        achievement.onPropertyChangeListener(event);

        assertFalse(achievement.onChangeValueCalled, "onChangeValue should NOT have been called for unrelated stat");
    }

    @Test
    @Order(12)
    @DisplayName("onPropertyChangeListener: disabled achievement is skipped entirely")
    void listener_disabledAchievementSkipped() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(false);

        StatPropertyUpdateEvent event = makeEvent(watchedStat, 50L, 0L);
        achievement.onPropertyChangeListener(event);

        assertFalse(achievement.onChangeValueCalled);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delta reconstruction
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(20)
    @DisplayName("delta reconstruction: effectiveOld = currentNew - rawDelta")
    void deltaReconstruction_isCorrect() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(true);

        // container currently reports 70 for watchedStat (after the increment)
        statsMap.put(realm, watchedStat, 70L, true);
        // raw delta = newValue - oldValue = 70 - 20 = 50
        StatPropertyUpdateEvent event = makeEvent(watchedStat, 70L, 20L);
        achievement.onPropertyChangeListener(event);

        assertTrue(achievement.onChangeValueCalled);
        // effectiveNew = getValue(container, watchedStat) = 70
        // effectiveOld = 70 - (70-20) = 20
        assertEquals(70L, achievement.capturedNewValue, "effectiveNew should be current map value");
        assertEquals(20L, achievement.capturedOldValue, "effectiveOld should be reconstructed from delta");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Threshold notifications
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(30)
    @DisplayName("handleNotify: fires at 50% threshold when crossing from below")
    void handleNotify_firesAt50Percent() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(true);

        // oldValue=40 (40%), newValue=55 (55%) — crosses the 50% threshold
        achievement.handleNotify(container, watchedStat, 55L, 40L, Map.of());
        assertEquals(1, achievement.notifyProgressCount, "Should notify once at 50% threshold");
    }

    @Test
    @Order(31)
    @DisplayName("handleNotify: does NOT fire when staying below threshold")
    void handleNotify_doesNotFireBelowThreshold() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(true);

        // oldValue=30 (30%), newValue=45 (45%) — still below 50% threshold
        achievement.handleNotify(container, watchedStat, 45L, 30L, Map.of());
        assertEquals(0, achievement.notifyProgressCount);
    }

    @Test
    @Order(32)
    @DisplayName("handleNotify: does NOT fire when old percent is already at threshold")
    void handleNotify_doesNotFireWhenAlreadyAtThreshold() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(true);

        // oldValue=50 (50%), newValue=60 (60%) — old is exactly at threshold, not below it
        achievement.handleNotify(container, watchedStat, 60L, 50L, Map.of());
        assertEquals(0, achievement.notifyProgressCount);
    }

    @Test
    @Order(33)
    @DisplayName("handleNotify: only fires once (first threshold crossed), not both")
    void handleNotify_onlyFirstThresholdFires() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(true);

        // oldValue=10 (10%), newValue=95 (95%) — crosses both 50% and 90% thresholds
        achievement.handleNotify(container, watchedStat, 95L, 10L, Map.of());
        assertEquals(1, achievement.notifyProgressCount, "Only the first crossed threshold should fire");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Completion
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(40)
    @DisplayName("handleComplete: complete() is called when at 100% and not yet completed")
    void handleComplete_callsCompleteWhenFull() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(true);
        statsMap.put(realm, watchedStat, 100L, true); // 100% — ALL aggregate = 100

        achievement.handleComplete(container);

        verify(mockAchievementManager).saveCompletion(eq(container), eq(achievement), isNull());
    }

    @Test
    @Order(41)
    @DisplayName("handleComplete: complete() is NOT called when already completed")
    void handleComplete_doesNotCallCompleteIfAlreadyDone() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(true);
        statsMap.put(realm, watchedStat, 100L, true); // 100%
        when(completions.getCompletion(any(), any()))
                .thenReturn(Optional.of(mock(AchievementCompletion.class)));

        achievement.handleComplete(container);

        verify(mockAchievementManager, never()).saveCompletion(any(), any(), any());
    }

    @Test
    @Order(42)
    @DisplayName("handleComplete: complete() is NOT called when < 100%")
    void handleComplete_doesNotCallCompleteWhenNotFull() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(true);
        statsMap.put(realm, watchedStat, 90L, true); // 90% — not yet done

        achievement.handleComplete(container);

        verify(mockAchievementManager, never()).saveCompletion(any(), any(), any());
    }

    @Test
    @Order(43)
    @DisplayName("handleComplete: complete() is called exactly once even on repeated calls")
    void handleComplete_calledExactlyOnce() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(true);
        statsMap.put(realm, watchedStat, 100L, true); // 100%

        // first call — not yet completed
        achievement.handleComplete(container);
        // second call — simulate completion now recorded
        when(completions.getCompletion(any(), any()))
                .thenReturn(Optional.of(mock(AchievementCompletion.class)));
        achievement.handleComplete(container);

        verify(mockAchievementManager, times(1)).saveCompletion(any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // forceCheck
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(50)
    @DisplayName("forceCheck: completes achievement when at 100% and not yet done")
    void forceCheck_completesWhenFull() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(true);
        statsMap.put(realm, watchedStat, 100L, true); // seed via realm fixture; ALL aggregate picks it up

        achievement.forceCheck(container);

        verify(mockAchievementManager).saveCompletion(eq(container), eq(achievement), isNull());
    }

    @Test
    @Order(51)
    @DisplayName("forceCheck: does nothing when already completed")
    void forceCheck_skipsWhenAlreadyCompleted() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(true);
        statsMap.put(realm, watchedStat, 100L, true); // seed via realm fixture
        when(completions.getCompletion(any(), any()))
                .thenReturn(Optional.of(mock(AchievementCompletion.class)));

        achievement.forceCheck(container);

        verify(mockAchievementManager, never()).saveCompletion(any(), any(), any());
    }

    @Test
    @Order(52)
    @DisplayName("forceCheck: does nothing when disabled")
    void forceCheck_skipsWhenDisabled() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(false);
        statsMap.put(realm, watchedStat, 100L, true); // seed via realm fixture

        achievement.forceCheck(container);

        verify(mockAchievementManager, never()).saveCompletion(any(), any(), any());
    }

    @Test
    @Order(53)
    @DisplayName("forceCheck: does nothing when progress < 100%")
    void forceCheck_skipsWhenNotFull() {
        TestAchievement achievement = makeAchievement(100L);
        achievement.setEnabled(true);
        statsMap.put(realm, watchedStat, 50L, true); // seed via realm fixture

        achievement.forceCheck(container);

        verify(mockAchievementManager, never()).saveCompletion(any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Performance
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(60)
    @DisplayName("performance: 10k sequential onChangeValue calls complete within 200ms")
    void performance_onChangeValueThroughput() {
        TestAchievement achievement = makeAchievement(100_000L);
        achievement.setEnabled(true);

        long start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            achievement.onChangeValue(container, watchedStat, (long) i, (long) (i - 1), Map.of());
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(elapsedMs < 200, "10k onChangeValue calls took too long: " + elapsedMs + "ms");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Concrete test subclass
    // ─────────────────────────────────────────────────────────────────────────

    static class TestAchievement extends Achievement {
        private final long goal;
        boolean onChangeValueCalled = false;
        long capturedNewValue = -1;
        long capturedOldValue = -1;
        int notifyProgressCount = 0;

        TestAchievement(String name, NamespacedKey key, StatFilterType filterType, long goal, IStat... stats) {
            super(name, key, null, filterType, stats);
            this.goal = goal;
            // Simulate loadConfig defaults
            setEnabled(true);
            // set notifyThresholds via the inherited field (package visible via reflection or just rely on loadConfig)
            // We call onChangeValue directly in some tests; set thresholds via reflection to avoid yaml dep.
            try {
                var field = Achievement.class.getDeclaredField("notifyThresholds");
                field.setAccessible(true);
                field.set(this, List.of(0.50f, 0.90f));
                var doRewards = Achievement.class.getDeclaredField("doRewards");
                doRewards.setAccessible(true);
                doRewards.set(this, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Expose enabled setter for tests */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public float calculatePercent(Map<IStat, Long> propertyMap) {
            Long current = propertyMap.get(watchedStatRef());
            if (current == null) return 0f;
            return Math.clamp((float) current / goal, 0f, 1f);
        }

        @Override
        public float getPercentComplete(StatContainer container, StatFilterType type, @Nullable Period period) {
            Long val = container.getStats().get(type, period, watchedStatRef());
            if (val == null) return 0f;
            return Math.clamp((float) val / goal, 0f, 1f);
        }

        private IStat watchedStatRef() {
            return getWatchedStats().iterator().next();
        }

        @Override
        public void onChangeValue(StatContainer container, IStat stat, Long newValue, Long oldValue, java.util.Map<IStat, Long> otherProperties) {
            onChangeValueCalled = true;
            capturedNewValue = newValue;
            capturedOldValue = oldValue;
            // Delegate to parent for threshold/completion logic, but skip StatPropertyUpdateEvent (no Bukkit PluginManager in tests)
            handleNotify(container, stat, newValue, oldValue, otherProperties);
            handleComplete(container);
        }

        @Override
        public void notifyProgress(StatContainer container, net.kyori.adventure.audience.Audience audience, float threshold) {
            notifyProgressCount++;
        }

        @Override
        public void notifyComplete(StatContainer container, net.kyori.adventure.audience.Audience audience) {
            // no-op for tests
        }

        @Override
        public @NotNull String getStatType() {
            return getNamespacedKey().asString();
        }
    }
}

