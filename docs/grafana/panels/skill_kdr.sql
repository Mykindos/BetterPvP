-- =============================================================================
-- Grafana Panel Query: Skill KDR
-- Data source: PostgreSQL (BetterPvP)
-- Panel type:  Bar Chart (vertical, sorted by KDR DESC)
--              + optional Time-series trend panel
--
-- Template variable required:
--   Name: realm   Type: Query   Query: SELECT DISTINCT realm FROM kills ORDER BY realm
-- =============================================================================

-- ── Option A: Live query ──────────────────────────────────────────────────────
WITH skill_kills AS (
    SELECT
        StatData->'wrappedStat'->>'skillName' AS skill_name,
        SUM(Stat)                             AS kills
    FROM client_stats
    WHERE StatType = 'CLANS_WRAPPER'
      AND StatData->'wrappedStat'->>'statType' = 'CHAMPIONS_SKILL'
      AND StatData->'wrappedStat'->>'action'   = 'KILL'
      AND StatData->'wrappedStat'->>'skillName' IS NOT NULL
      AND StatData->'wrappedStat'->>'skillName' != ''
      AND Realm = $realm
    GROUP BY StatData->'wrappedStat'->>'skillName'
),
skill_deaths AS (
    SELECT
        StatData->'wrappedStat'->>'skillName' AS skill_name,
        SUM(Stat)                             AS deaths
    FROM client_stats
    WHERE StatType = 'CLANS_WRAPPER'
      AND StatData->'wrappedStat'->>'statType' = 'CHAMPIONS_SKILL'
      AND StatData->'wrappedStat'->>'action'   = 'DEATH'
      AND StatData->'wrappedStat'->>'skillName' IS NOT NULL
      AND StatData->'wrappedStat'->>'skillName' != ''
      AND Realm = $realm
    GROUP BY StatData->'wrappedStat'->>'skillName'
),
skill_time AS (
    SELECT
        StatData->'wrappedStat'->>'skillName'  AS skill_name,
        SUM(Stat)                              AS time_played_ms
    FROM client_stats
    WHERE StatType = 'CLANS_WRAPPER'
      AND StatData->'wrappedStat'->>'statType' = 'CHAMPIONS_SKILL'
      AND StatData->'wrappedStat'->>'action'   = 'TIME_PLAYED'
      AND StatData->'wrappedStat'->>'skillName' IS NOT NULL
      AND StatData->'wrappedStat'->>'skillName' != ''
      AND Realm = $realm
    GROUP BY StatData->'wrappedStat'->>'skillName'
)
SELECT
    k.skill_name                                                        AS "Skill",
    k.kills                                                             AS "Kills",
    COALESCE(d.deaths, 0)                                               AS "Deaths",
    CASE
        WHEN COALESCE(d.deaths, 0) = 0 THEN k.kills::NUMERIC
        ELSE ROUND(k.kills::NUMERIC / d.deaths, 2)
    END                                                                 AS "KDR",
    ROUND(COALESCE(t.time_played_ms, 0) / 3600000.0, 2)                AS "Hours Played"
FROM skill_kills k
LEFT JOIN skill_deaths d ON k.skill_name = d.skill_name
LEFT JOIN skill_time   t ON k.skill_name = t.skill_name
ORDER BY "KDR" DESC;


-- ── Option B: Latest snapshot ─────────────────────────────────────────────────
SELECT
    skill_name                                      AS "Skill",
    kills                                           AS "Kills",
    deaths                                          AS "Deaths",
    kdr                                             AS "KDR",
    ROUND(time_played_ms / 3600000.0, 2)            AS "Hours Played"
FROM skill_kdr_snapshot
WHERE realm = $realm
  AND snapshot_time = (
        SELECT MAX(snapshot_time)
        FROM   skill_kdr_snapshot
        WHERE  realm = $realm
      )
ORDER BY kdr DESC;


-- ── Option C: KDR trend over time for a specific skill (time-series) ──────────
-- Add a template variable $skill (query: SELECT DISTINCT skill_name FROM skill_kdr_snapshot)

SELECT
    snapshot_time   AS "time",
    kdr
FROM skill_kdr_snapshot
WHERE realm      = $realm
  AND skill_name = '$skill'
  AND $__timeFilter(snapshot_time)
ORDER BY snapshot_time ASC;


-- ── Option D: All skills KDR trend (multi-series) ─────────────────────────────
SELECT
    snapshot_time   AS "time",
    skill_name      AS "metric",
    kdr
FROM skill_kdr_snapshot
WHERE realm = $realm
  AND $__timeFilter(snapshot_time)
ORDER BY snapshot_time ASC, skill_name ASC;

