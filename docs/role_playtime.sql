-- Playtime, kills, deaths, and KDR per role (all players aggregated) for a specific realm.
-- Time played is sourced from client_stats (CLANS_WRAPPER / CHAMPIONS_ROLE).
-- Kills and deaths are sourced from the kills + champions_kills tables.
-- Replace 6 with the target realm ID (integer).
WITH role_time AS (
    SELECT
        StatData->'wrappedStat'->>'role' AS role,
        SUM(Stat)                        AS time_played_ms
    FROM client_stats
    WHERE StatType = 'CLANS_WRAPPER'
      AND StatData->'wrappedStat'->>'statType' = 'CHAMPIONS_ROLE'
      AND StatData->'wrappedStat'->>'action'   = 'TIME_PLAYED'
      AND Realm = 6
    GROUP BY StatData->'wrappedStat'->>'role'
),
role_kills AS (
    SELECT
        ck.killer_class AS role,
        COUNT(*)        AS kills
    FROM kills k
    JOIN champions_kills ck ON k.id = ck.kill_id
    WHERE k.realm = 6
      AND k.valid = TRUE
      AND ck.killer_class <> ''
    GROUP BY ck.killer_class
),
role_deaths AS (
    SELECT
        ck.victim_class AS role,
        COUNT(*)        AS deaths
    FROM kills k
    JOIN champions_kills ck ON k.id = ck.kill_id
    WHERE k.realm = 6
      AND k.valid = TRUE
      AND ck.victim_class <> ''
    GROUP BY ck.victim_class
)
SELECT
    t.role,
    COALESCE(k.kills,   0)                                            AS kills,
    COALESCE(d.deaths,  0)                                            AS deaths,
    CASE
        WHEN COALESCE(d.deaths, 0) = 0 THEN COALESCE(k.kills, 0)::NUMERIC
        ELSE ROUND(COALESCE(k.kills, 0)::NUMERIC / d.deaths, 2)
    END                                                               AS kdr,
    t.time_played_ms,
    TO_CHAR(
        make_interval(secs => t.time_played_ms / 1000.0),
        'HH24:MI:SS'
    )                                                                 AS time_played
FROM role_time t
LEFT JOIN role_kills  k ON t.role = k.role
LEFT JOIN role_deaths d ON t.role = d.role
ORDER BY t.time_played_ms DESC;

