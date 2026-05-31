-- =============================================================================
-- Grafana Panel Query: Weapon Damage Range & DPS
-- Data source: PostgreSQL (BetterPvP)
-- Requires: weapon_damage_config table + weapon_damage_permutations VIEW
--           (both created by schema.sql; data from weapon_seed.sql)
--
-- Panel type options:
--   1. Bar Chart  — shows Min / Base / Max as three bars per weapon config
--   2. Candlestick — maps Low=min, Open=base, Close=base, High=max
--
-- Template variables:
--   weapon_type:  Custom variable  Options: All,sword,axe,special
-- =============================================================================

-- ── Query 1: Damage range per weapon config ───────────────────────────────────
-- Recommended panel: Bar Chart
--   X-axis field: config_label
--   Series: effective_min (Min), effective_base (Base), effective_max (Max)
--   Sort by effective_base ASC in Grafana transforms, or use ORDER BY here.

SELECT
    config_label                AS "Weapon Config",
    effective_min               AS "Min Damage",
    effective_base              AS "Base Damage",
    effective_max               AS "Max Damage"
FROM weapon_damage_permutations
WHERE (
    '${weapon_type}' = 'All'
    OR weapon_type = '${weapon_type}'
)
  AND effective_base > 0          -- exclude ability-only weapons (Meridian Scepter)
ORDER BY weapon_type, effective_base ASC, rune_count ASC;


-- ── Query 2: DPS range per weapon config ─────────────────────────────────────
-- Recommended panel: Bar Chart (separate from damage panel, or second tab)
--   X-axis field: config_label
--   Series: dps_min, dps_base, dps_max
--
-- DPS formula:  damage × (1000 / (400 / (1 + attack_speed_base)))
--             = damage × 2.5 × (1 + attack_speed_base)
-- DEFAULT_DELAY = 400ms  (from DamageCause.java)

SELECT
    config_label                AS "Weapon Config",
    dps_min                     AS "DPS Min",
    dps_base                    AS "DPS Base",
    dps_max                     AS "DPS Max"
FROM weapon_damage_permutations
WHERE (
    '${weapon_type}' = 'All'
    OR weapon_type = '${weapon_type}'
)
  AND effective_base > 0
ORDER BY weapon_type, dps_base ASC, rune_count ASC;


-- ── Query 3: Full-range DPS (worst-case / best-case, including attack speed roll) ──
-- Includes the effect of getting a min or max attack-speed roll as well.
-- Recommended panel: Bar Chart or Stat

SELECT
    config_label                AS "Weapon Config",
    dps_worst                   AS "DPS (min dmg + min spd)",
    dps_base                    AS "DPS (base)",
    dps_best                    AS "DPS (max dmg + max spd)"
FROM weapon_damage_permutations
WHERE (
    '${weapon_type}' = 'All'
    OR weapon_type = '${weapon_type}'
)
  AND effective_base > 0
ORDER BY weapon_type, dps_base ASC, rune_count ASC;


-- ── Query 4: Candlestick-compatible format ────────────────────────────────────
-- For Grafana Candlestick panel (Grafana 10+, non-time mode):
--   Enable "Use time field for X axis" → OFF (if your Grafana version supports it)
--   OR use a synthetic integer as the time field and map config_label to X-axis label.
--
-- Field mapping:
--   time  → sequence (use as X-axis order)
--   open  → effective_base   (centre line)
--   high  → effective_max    (top wick)
--   low   → effective_min    (bottom wick)
--   close → effective_base   (same as open — no real OHLC concept here)

SELECT
    ROW_NUMBER() OVER (
        ORDER BY weapon_type, effective_base ASC, rune_count ASC
    )                           AS "time",     -- synthetic X-axis order
    config_label                AS "label",
    effective_base              AS "open",
    effective_max               AS "high",
    effective_min               AS "low",
    effective_base              AS "close"
FROM weapon_damage_permutations
WHERE (
    '${weapon_type}' = 'All'
    OR weapon_type = '${weapon_type}'
)
  AND effective_base > 0;


-- ── Notes on panel configuration ─────────────────────────────────────────────
-- Bar Chart (damage or DPS):
--   Visualization → Bar Chart
--   X-axis → "Weapon Config"
--   Values → select Min / Base / Max series
--   Orientation → Vertical (or Horizontal for longer labels)
--   Legend → Show (to distinguish Min/Base/Max)
--
-- Candlestick:
--   Visualization → Candlestick
--   Open field  → "open"
--   High field  → "high"
--   Low field   → "low"
--   Close field → "close"
--   In Grafana 10.3+ you can override the X-axis label field to "label".
--   In older versions, use the Bar Chart query instead.

