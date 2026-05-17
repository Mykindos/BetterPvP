-- Playtime per role (all players aggregated) for a specific realm.
-- Stats are stored as CLANS_WRAPPER rows where the RoleStat is nested
-- one level deep inside StatData->'wrappedStat'.
-- Replace 6 with the target realm ID (integer).
SELECT
    StatData->'wrappedStat'->>'role'          AS role,
    SUM(Stat)                                 AS time_played_ms,
    TO_CHAR(
        make_interval(secs => SUM(Stat) / 1000.0),
        'HH24:MI:SS'
    )                                         AS time_played
FROM client_stats
WHERE StatType = 'CLANS_WRAPPER'
  AND StatData->'wrappedStat'->>'statType' = 'CHAMPIONS_ROLE'
  AND StatData->'wrappedStat'->>'action'   = 'TIME_PLAYED'
  AND Realm = 6
GROUP BY StatData->'wrappedStat'->>'role'
ORDER BY time_played_ms DESC;

