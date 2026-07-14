-- Kills, deaths, and KDR for every class matchup for a specific realm.
-- Each row represents: <role> attacking <vs_role>.
-- Deaths for a given row = kills in the reverse matchup (vs_role attacking role).
-- Replace 6 with the target realm ID (integer).
WITH matchup_kills AS (
    SELECT
        ck.killer_class AS attacker,
        ck.victim_class AS defender,
        COUNT(*)        AS kills
    FROM kills k
    JOIN champions_kills ck ON k.id = ck.kill_id
    WHERE k.realm = 6
      AND k.valid = TRUE
      AND ck.killer_class <> ''
      AND ck.victim_class <> ''
    GROUP BY ck.killer_class, ck.victim_class
)
SELECT
    a.attacker                                                          AS role,
    a.defender                                                          AS vs_role,
    a.kills,
    COALESCE(b.kills, 0)                                                AS deaths,
    CASE
        WHEN COALESCE(b.kills, 0) = 0 THEN a.kills::NUMERIC
        ELSE ROUND(a.kills::NUMERIC / b.kills, 2)
    END                                                                 AS kdr
FROM matchup_kills a
LEFT JOIN matchup_kills b ON a.attacker = b.defender
                          AND a.defender = b.attacker
ORDER BY a.attacker, kdr DESC;

