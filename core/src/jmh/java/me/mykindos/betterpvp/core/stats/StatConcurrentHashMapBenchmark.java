package me.mykindos.betterpvp.core.stats;

import me.mykindos.betterpvp.core.client.stats.StatConcurrentHashMap;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.IWrapperStat;
import me.mykindos.betterpvp.core.client.stats.impl.utility.StatValueType;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * JMH micro-benchmarks for {@link StatConcurrentHashMap}.
 *
 * <p>Run with: {@code ./gradlew :core:jmh}</p>
 *
 * <p>These benchmarks replace the wall-clock assertions that were previously in
 * {@code StatConcurrentHashMapTest} to avoid flaky CI failures from environment-dependent timing.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class StatConcurrentHashMapBenchmark {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static IStat savableLeafStat(String type) {
        return new IStat() {
            @Override public @NotNull String getStatType() { return type; }
            @Override public Long getStat(me.mykindos.betterpvp.core.client.stats.StatContainer c, StatFilterType t, @Nullable Period p) { return 0L; }
            @Override public @NotNull StatValueType getStatValueType() { return StatValueType.LONG; }
            @Override public JSONObject getJsonData() { return null; }
            @Override public boolean isSavable() { return true; }
            @Override public boolean containsStat(IStat o) { return equals(o); }
            @Override public @NotNull IStat getGenericStat() { return this; }
            @Override public boolean equals(Object o) { return o instanceof IStat s && type.equals(s.getStatType()); }
            @Override public int hashCode() { return type.hashCode(); }
        };
    }

    private static IWrapperStat cheapWrapper(IStat base) {
        return new IWrapperStat() {
            @Override public IStat getWrappedStat() { return base; }
            @Override public @NotNull String getStatType() { return "w_" + base.getStatType() + "_" + System.identityHashCode(this); }
            @Override public Long getStat(me.mykindos.betterpvp.core.client.stats.StatContainer c, StatFilterType t, @Nullable Period p) { return 0L; }
            @Override public @NotNull StatValueType getStatValueType() { return StatValueType.LONG; }
            @Override public JSONObject getJsonData() { return null; }
            @Override public boolean isSavable() { return true; }
            @Override public boolean containsStat(IStat o) { return equals(o); }
            @Override public @NotNull IStat getGenericStat() { return this; }
        };
    }

    // ── State: heavy 11 250-entry map (stat-menu scenario) ───────────────────

    /**
     * Simulates a veteran player's stat map:
     * 5 seasons × 3 realms = 15 {@link Realm} entries, 30 base stats,
     * 25 wrapper variants each → 11 250 primary map entries.
     *
     * <p>Models the O(1) {@code getLeafAggregate} path used by the stat menu.</p>
     */
    @State(Scope.Benchmark)
    public static class HeavyMapState {
        StatConcurrentHashMap map;
        IStat[] baseStats;

        @Setup
        public void setup() {
            final int GENERIC_COUNT     = 30;
            final int SEASON_COUNT      = 5;
            final int REALMS_PER_SEASON = 3;
            final int WRAPPERS_PER_BASE = 25;

            Season[] seasons = new Season[SEASON_COUNT];
            Realm[] realms   = new Realm[SEASON_COUNT * REALMS_PER_SEASON];
            for (int s = 0; s < SEASON_COUNT; s++) {
                seasons[s] = new Season(s + 1, "Season_" + s, LocalDate.now().plusYears(s));
                for (int r = 0; r < REALMS_PER_SEASON; r++) {
                    int idx = s * REALMS_PER_SEASON + r;
                    realms[idx] = new Realm(idx + 1, new Server(idx + 1, "srv-" + idx), seasons[s]);
                }
            }

            baseStats = new IStat[GENERIC_COUNT];
            for (int i = 0; i < GENERIC_COUNT; i++) {
                baseStats[i] = savableLeafStat("base_" + i);
            }

            map = new StatConcurrentHashMap();
            for (Realm rlm : realms) {
                for (int g = 0; g < GENERIC_COUNT; g++) {
                    for (int w = 0; w < WRAPPERS_PER_BASE; w++) {
                        map.put(rlm, cheapWrapper(baseStats[g]), (long) (g + w + 1), true);
                    }
                }
            }
        }
    }

    /**
     * Benchmarks O(1) {@code getLeafAggregate} across 30 stats on an 11 250-entry map.
     * Equivalent to one stat-menu render pass for a veteran player.
     *
     * <p>The old O(n) {@code getFilteredStats} path would perform ~337 500 iterations for the
     * same 30-stat render pass; this benchmark quantifies the improvement.</p>
     */
    @Benchmark
    public void leafAggregateLookup_30Stats_11250Entries(HeavyMapState state, Blackhole bh) {
        for (IStat stat : state.baseStats) {
            bh.consume(state.map.getLeafAggregate(StatFilterType.ALL, null, stat));
        }
    }

    // ── State: SkillStatListener tick (100 players, 5 skills each) ───────────

    /**
     * Simulates one {@code SkillStatListener.doUpdateEvent()} tick:
     * 100 online players, each with 5 equipped skills → 500 {@code increase()} calls.
     *
     * <p>Each write updates 6 secondary indexes, so 500 calls = 3 000 map operations.</p>
     */
    @State(Scope.Benchmark)
    public static class SkillTickState {
        StatConcurrentHashMap[] playerMaps;
        IStat[] equippedSkills;
        Realm realm;

        @Setup
        public void setup() {
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

            Season currentSeason = new Season(1, "Current", LocalDate.now());
            realm = new Realm(1, new Server(1, "current"), currentSeason);

            playerMaps = new StatConcurrentHashMap[PLAYER_COUNT];
            for (int p = 0; p < PLAYER_COUNT; p++) {
                playerMaps[p] = new StatConcurrentHashMap();
                for (Realm rlm : priorRealms) {
                    for (int s = 0; s < PRIOR_SKILLS; s++) {
                        for (int l = 1; l <= PRIOR_LEVELS; l++) {
                            IStat skillStat = savableLeafStat("SKILL_OldSkill_" + s + "_" + l);
                            playerMaps[p].put(rlm, skillStat, 60_000L * l, true);
                        }
                    }
                }
            }

            equippedSkills = new IStat[SKILLS_IN_BUILD];
            for (int s = 0; s < SKILLS_IN_BUILD; s++) {
                equippedSkills[s] = savableLeafStat("SKILL_Skill_" + s + "_3");
            }
        }
    }

    /**
     * Benchmarks 500 {@code increase()} calls (100 players × 5 skills),
     * each updating 6 secondary indexes — representative of one tick of
     * {@code SkillStatListener.doUpdateEvent()}.
     */
    @Benchmark
    public void skillStatTick_100Players_5Skills(SkillTickState state) {
        for (StatConcurrentHashMap playerMap : state.playerMaps) {
            for (IStat skill : state.equippedSkills) {
                playerMap.increase(state.realm, skill, 1000L);
            }
        }
    }
}

