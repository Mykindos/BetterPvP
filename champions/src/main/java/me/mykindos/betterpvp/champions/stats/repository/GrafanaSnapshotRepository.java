package me.mykindos.betterpvp.champions.stats.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import org.jooq.DSLContext;

/**
 * Periodically snapshots Champions analytics data into dedicated Grafana tables.
 *
 * <p>Each snapshot captures the <em>cumulative</em> realm totals at that point in time,
 * letting Grafana show both the current state (latest snapshot) and historical trends
 * (time-series over all snapshots).</p>
 *
 * <p>Scheduled in {@link me.mykindos.betterpvp.champions.Champions#onEnable()} via
 * {@code UtilServer.runTaskTimerAsync}.</p>
 */
@Singleton
@CustomLog
public class GrafanaSnapshotRepository {

    private final Database database;

    @Inject
    public GrafanaSnapshotRepository(Database database) {
        this.database = database;
    }

    /**
     * Inserts one snapshot row per tracked entity (role matchup, role playtime, skill)
     * for the given realm into the three Grafana snapshot tables.
     *
     * @param realmId the current realm ID (from {@link me.mykindos.betterpvp.core.Core#getCurrentRealm()})
     */
    public void takeSnapshot(int realmId) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            takeRoleMatchupSnapshot(ctx, realmId);
            takeRolePlaytimeSnapshot(ctx, realmId);
            takeSkillKdrSnapshot(ctx, realmId);
            log.info("Grafana snapshot taken for realm {}", realmId).submit();
        }).exceptionally(ex -> {
            log.error("Failed to take Grafana snapshot for realm {}", realmId, ex).submit();
            return null;
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void takeRoleMatchupSnapshot(DSLContext ctx, int realmId) {
        ctx.execute("""
                WITH matchup_kills AS (
                    SELECT
                        ck.killer_class AS attacker,
                        ck.victim_class AS defender,
                        COUNT(*)        AS kills
                    FROM kills k
                    JOIN champions_kills ck ON k.id = ck.kill_id
                    WHERE k.realm  = ?
                      AND k.valid  = TRUE
                      AND ck.killer_class <> ''
                      AND ck.victim_class <> ''
                    GROUP BY ck.killer_class, ck.victim_class
                )
                INSERT INTO grafana_role_matchup_snapshot
                    (realm, captured_at, role, vs_role, kills, deaths, kdr)
                SELECT
                    ?::integer,
                    NOW(),
                    a.attacker,
                    a.defender,
                    a.kills,
                    COALESCE(b.kills, 0),
                    CASE
                        WHEN COALESCE(b.kills, 0) = 0 THEN a.kills::NUMERIC
                        ELSE ROUND(a.kills::NUMERIC / b.kills, 2)
                    END
                FROM matchup_kills a
                LEFT JOIN matchup_kills b
                    ON a.attacker = b.defender
                   AND a.defender = b.attacker
                """,
                realmId, realmId);
    }

    private void takeRolePlaytimeSnapshot(DSLContext ctx, int realmId) {
        ctx.execute("""
                WITH role_time AS (
                    SELECT
                        statdata->'wrappedStat'->>'role' AS role,
                        SUM(stat)                        AS time_played_ms
                    FROM client_stats
                    WHERE stattype = 'CLANS_WRAPPER'
                      AND statdata->'wrappedStat'->>'statType' = 'CHAMPIONS_ROLE'
                      AND statdata->'wrappedStat'->>'action'   = 'TIME_PLAYED'
                      AND realm = ?
                    GROUP BY statdata->'wrappedStat'->>'role'
                ),
                role_kills AS (
                    SELECT
                        ck.killer_class AS role,
                        COUNT(*)        AS kills
                    FROM kills k
                    JOIN champions_kills ck ON k.id = ck.kill_id
                    WHERE k.realm  = ?
                      AND k.valid  = TRUE
                      AND ck.killer_class <> ''
                    GROUP BY ck.killer_class
                ),
                role_deaths AS (
                    SELECT
                        ck.victim_class AS role,
                        COUNT(*)        AS deaths
                    FROM kills k
                    JOIN champions_kills ck ON k.id = ck.kill_id
                    WHERE k.realm  = ?
                      AND k.valid  = TRUE
                      AND ck.victim_class <> ''
                    GROUP BY ck.victim_class
                )
                INSERT INTO grafana_role_playtime_snapshot
                    (realm, captured_at, role, kills, deaths, kdr, time_played_ms)
                SELECT
                    ?::integer,
                    NOW(),
                    t.role,
                    COALESCE(k.kills,  0),
                    COALESCE(d.deaths, 0),
                    CASE
                        WHEN COALESCE(d.deaths, 0) = 0 THEN COALESCE(k.kills, 0)::NUMERIC
                        ELSE ROUND(COALESCE(k.kills, 0)::NUMERIC / d.deaths, 2)
                    END,
                    t.time_played_ms
                FROM role_time t
                LEFT JOIN role_kills  k ON t.role = k.role
                LEFT JOIN role_deaths d ON t.role = d.role
                """,
                realmId, realmId, realmId, realmId);
    }

    private void takeSkillKdrSnapshot(DSLContext ctx, int realmId) {
        ctx.execute("""
                WITH skill_kills AS (
                    SELECT
                        statdata->'wrappedStat'->>'skillName' AS skill_name,
                        SUM(stat)                             AS kills
                    FROM client_stats
                    WHERE stattype = 'CLANS_WRAPPER'
                      AND statdata->'wrappedStat'->>'statType' = 'CHAMPIONS_SKILL'
                      AND statdata->'wrappedStat'->>'action'   = 'KILL'
                      AND statdata->'wrappedStat'->>'skillName' IS NOT NULL
                      AND statdata->'wrappedStat'->>'skillName' != ''
                      AND realm = ?
                    GROUP BY statdata->'wrappedStat'->>'skillName'
                ),
                skill_deaths AS (
                    SELECT
                        statdata->'wrappedStat'->>'skillName' AS skill_name,
                        SUM(stat)                             AS deaths
                    FROM client_stats
                    WHERE stattype = 'CLANS_WRAPPER'
                      AND statdata->'wrappedStat'->>'statType' = 'CHAMPIONS_SKILL'
                      AND statdata->'wrappedStat'->>'action'   = 'DEATH'
                      AND statdata->'wrappedStat'->>'skillName' IS NOT NULL
                      AND statdata->'wrappedStat'->>'skillName' != ''
                      AND realm = ?
                    GROUP BY statdata->'wrappedStat'->>'skillName'
                ),
                skill_time AS (
                    SELECT
                        statdata->'wrappedStat'->>'skillName' AS skill_name,
                        SUM(stat)                             AS time_played_ms
                    FROM client_stats
                    WHERE stattype = 'CLANS_WRAPPER'
                      AND statdata->'wrappedStat'->>'statType' = 'CHAMPIONS_SKILL'
                      AND statdata->'wrappedStat'->>'action'   = 'TIME_PLAYED'
                      AND statdata->'wrappedStat'->>'skillName' IS NOT NULL
                      AND statdata->'wrappedStat'->>'skillName' != ''
                      AND realm = ?
                    GROUP BY statdata->'wrappedStat'->>'skillName'
                )
                INSERT INTO grafana_skill_kdr_snapshot
                    (realm, captured_at, skill_name, kills, deaths, kdr, time_played_ms)
                SELECT
                    ?::integer,
                    NOW(),
                    k.skill_name,
                    k.kills,
                    COALESCE(d.deaths, 0),
                    CASE
                        WHEN COALESCE(d.deaths, 0) = 0 THEN k.kills::NUMERIC
                        ELSE ROUND(k.kills::NUMERIC / d.deaths, 2)
                    END,
                    COALESCE(t.time_played_ms, 0)
                FROM skill_kills k
                LEFT JOIN skill_deaths d ON k.skill_name = d.skill_name
                LEFT JOIN skill_time   t ON k.skill_name = t.skill_name
                """,
                realmId, realmId, realmId, realmId);
    }
}

