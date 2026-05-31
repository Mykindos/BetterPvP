-- =============================================================================
-- Grafana Panel Query: Role Matchup KDR
-- Data source: PostgreSQL (BetterPvP)
-- Panel type:  Table  (conditional colour on KDR column)
--              OR Heatmap (see note below)
--
-- Template variable required:
--   Name: realm   Type: Query   Query: SELECT DISTINCT realm FROM kills ORDER BY realm
-- =============================================================================

-- ── Option A: Live query ──────────────────────────────────────────────────────
-- Use this if you only need the current totals (no history).
-- Paste directly into a Table panel.

WITH matchup_kills AS (
    SELECT
        ck.killer_class AS attacker,
        ck.victim_class AS defender,
        COUNT(*)        AS kills
    FROM kills k
    JOIN champions_kills ck ON k.id = ck.kill_id
    WHERE k.realm = $realm
      AND k.valid = TRUE
      AND ck.killer_class <> ''
      AND ck.victim_class <> ''
    GROUP BY ck.killer_class, ck.victim_class
)
SELECT
    a.attacker                                                          AS "Role",
    a.defender                                                          AS "vs Role",
    a.kills                                                             AS "Kills",
    COALESCE(b.kills, 0)                                                AS "Deaths",
    CASE
        WHEN COALESCE(b.kills, 0) = 0 THEN a.kills::NUMERIC
        ELSE ROUND(a.kills::NUMERIC / b.kills, 2)
    END                                                                 AS "KDR"
FROM matchup_kills a
LEFT JOIN matchup_kills b ON a.attacker = b.defender
                          AND a.defender = b.attacker
ORDER BY a.attacker, "KDR" DESC;


-- ── Option B: Latest snapshot ─────────────────────────────────────────────────
-- Use this to show the most-recent snapshot (includes history in KDR trend panel).

SELECT
    role          AS "Role",
    vs_role       AS "vs Role",
    kills         AS "Kills",
    deaths        AS "Deaths",
    kdr           AS "KDR"
FROM role_matchup_snapshot
WHERE realm = $realm
  AND snapshot_time = (
        SELECT MAX(snapshot_time)
        FROM   role_matchup_snapshot
        WHERE  realm = $realm
      )
ORDER BY role, kdr DESC;


-- ── Option C: KDR trend over time (time-series, one attacker/defender pair) ───
-- Use this for a Time-series panel to track how a specific matchup KDR changes.
-- Add template variables: $attacker and $defender (query variable from kills).

SELECT
    snapshot_time AS "time",
    kdr
FROM role_matchup_snapshot
WHERE realm      = $realm
  AND role       = '$attacker'
  AND vs_role    = '$defender'
  AND $__timeFilter(snapshot_time)
ORDER BY snapshot_time ASC;


-- ── Heatmap note ──────────────────────────────────────────────────────────────
-- Grafana's native Heatmap panel expects (time, bucket, value).
-- For a role × role KDR matrix you have two options:
--   1. Use a Table panel with "Color by value" field override on the KDR column
--      (set thresholds: 0=red, 1=green) — this gives a colour-coded matrix.
--   2. Use the Canvas panel and manually lay out the grid.
-- Option 1 is by far the simplest and most readable.

