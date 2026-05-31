-- =============================================================================
-- Grafana Panel Query: Role Playtime
-- Data source: PostgreSQL (BetterPvP)
-- Panel type:  Bar Chart (horizontal, sorted by hours_played DESC)
--              + optional Time-series panel for trend view
--
-- Template variable required:
--   Name: realm   Type: Query   Query: SELECT DISTINCT realm FROM kills ORDER BY realm
-- =============================================================================

-- ── Option A: Live query ──────────────────────────────────────────────────────
WITH role_time AS (
    SELECT
        StatData->'wrappedStat'->>'role' AS role,
        SUM(Stat)                        AS time_played_ms
    FROM client_stats
    WHERE StatType = 'CLANS_WRAPPER'
      AND StatData->'wrappedStat'->>'statType' = 'CHAMPIONS_ROLE'
      AND StatData->'wrappedStat'->>'action'   = 'TIME_PLAYED'
      AND Realm = $realm
    GROUP BY StatData->'wrappedStat'->>'role'
),
role_kills AS (
    SELECT ck.killer_class AS role, COUNT(*) AS kills
    FROM kills k
    JOIN champions_kills ck ON k.id = ck.kill_id
    WHERE k.realm = $realm AND k.valid = TRUE AND ck.killer_class <> ''
    GROUP BY ck.killer_class
),
role_deaths AS (
    SELECT ck.victim_class AS role, COUNT(*) AS deaths
    FROM kills k
    JOIN champions_kills ck ON k.id = ck.kill_id
    WHERE k.realm = $realm AND k.valid = TRUE AND ck.victim_class <> ''
    GROUP BY ck.victim_class
)
SELECT
    t.role                                                              AS "Role",
    COALESCE(k.kills,  0)                                               AS "Kills",
    COALESCE(d.deaths, 0)                                               AS "Deaths",
    CASE
        WHEN COALESCE(d.deaths, 0) = 0 THEN COALESCE(k.kills, 0)::NUMERIC
        ELSE ROUND(COALESCE(k.kills, 0)::NUMERIC / d.deaths, 2)
    END                                                                 AS "KDR",
    ROUND(t.time_played_ms / 3600000.0, 2)                             AS "Hours Played"
FROM role_time t
LEFT JOIN role_kills  k ON t.role = k.role
LEFT JOIN role_deaths d ON t.role = d.role
ORDER BY t.time_played_ms DESC;


-- ── Option B: Latest snapshot ─────────────────────────────────────────────────
SELECT
    role                                            AS "Role",
    kills                                           AS "Kills",
    deaths                                          AS "Deaths",
    kdr                                             AS "KDR",
    ROUND(time_played_ms / 3600000.0, 2)            AS "Hours Played"
FROM role_playtime_snapshot
WHERE realm = $realm
  AND snapshot_time = (
        SELECT MAX(snapshot_time)
        FROM   role_playtime_snapshot
        WHERE  realm = $realm
      )
ORDER BY time_played_ms DESC;


-- ── Option C: Hours-played trend over time (time-series) ─────────────────────
-- Each role is a separate series; use "Group by → role" in Grafana transform
-- or use the multi-series format below.

SELECT
    snapshot_time                       AS "time",
    role                                AS "metric",
    ROUND(time_played_ms / 3600000.0, 2) AS "Hours Played"
FROM role_playtime_snapshot
WHERE realm = $realm
  AND $__timeFilter(snapshot_time)
ORDER BY snapshot_time ASC, role ASC;


-- ── Option D: KDR trend over time (time-series) ──────────────────────────────
SELECT
    snapshot_time   AS "time",
    role            AS "metric",
    kdr
FROM role_playtime_snapshot
WHERE realm = $realm
  AND $__timeFilter(snapshot_time)
ORDER BY snapshot_time ASC, role ASC;

