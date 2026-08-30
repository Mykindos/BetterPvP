-- Kill/Death Ratio per skill (all levels aggregated) for a specific realm.
-- Stats are stored as CLANS_WRAPPER rows where the ChampionsSkillStat is nested
-- one level deep inside StatData->'wrappedStat'.
-- Replace :realm_id with the target realm ID (integer).
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
      AND Realm = 6
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
      AND Realm = 6
    GROUP BY StatData->'wrappedStat'->>'skillName'
),
skill_time_played AS (
    SELECT
        StatData->'wrappedStat'->>'skillName'  AS skill_name,
        SUM(Stat)                              AS time_played_ms
    FROM client_stats
    WHERE StatType = 'CLANS_WRAPPER'
      AND StatData->'wrappedStat'->>'statType' = 'CHAMPIONS_SKILL'
      AND StatData->'wrappedStat'->>'action'   = 'TIME_PLAYED'
      AND StatData->'wrappedStat'->>'skillName' IS NOT NULL
      AND StatData->'wrappedStat'->>'skillName' != ''
      AND Realm = 6
    GROUP BY StatData->'wrappedStat'->>'skillName'
)
SELECT
    k.skill_name,
    k.kills,
    COALESCE(d.deaths, 0)                                             AS deaths,
    CASE
        WHEN COALESCE(d.deaths, 0) = 0 THEN k.kills::NUMERIC
        ELSE ROUND(k.kills::NUMERIC / d.deaths, 2)
    END                                                               AS kdr,
    COALESCE(t.time_played_ms, 0)                                     AS time_played_ms,
    TO_CHAR(
        make_interval(secs => COALESCE(t.time_played_ms, 0) / 1000.0),
        'HH24:MI:SS'
    )                                                                 AS time_played
FROM skill_kills k
LEFT JOIN skill_deaths d     ON k.skill_name = d.skill_name
LEFT JOIN skill_time_played t ON k.skill_name = t.skill_name
ORDER BY kdr DESC;


