-- =============================================================================
-- BetterPvP Grafana Integration Schema  (reference document)
-- The authoritative SQL lives in the Flyway migrations:
--   V20260530_1__Create_grafana_snapshot_tables.sql
--   V20260530_2__Create_grafana_config_tables.sql
-- This file documents the full intended schema for manual reference / local dev.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- ANALYTICS SNAPSHOTS  (periodic time-series data)
-- Created by V20260530_1__Create_grafana_snapshot_tables.sql
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS grafana_role_matchup_snapshot (
    id          BIGSERIAL     PRIMARY KEY,
    realm       INTEGER       NOT NULL,
    captured_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    role        VARCHAR(100)  NOT NULL,
    vs_role     VARCHAR(100)  NOT NULL,
    kills       BIGINT        NOT NULL DEFAULT 0,
    deaths      BIGINT        NOT NULL DEFAULT 0,
    kdr         NUMERIC(10,2) NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_grafana_role_matchup_realm_ts
    ON grafana_role_matchup_snapshot (realm, captured_at DESC);

CREATE TABLE IF NOT EXISTS grafana_role_playtime_snapshot (
    id             BIGSERIAL     PRIMARY KEY,
    realm          INTEGER       NOT NULL,
    captured_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    role           VARCHAR(100)  NOT NULL,
    kills          BIGINT        NOT NULL DEFAULT 0,
    deaths         BIGINT        NOT NULL DEFAULT 0,
    kdr            NUMERIC(10,2) NOT NULL DEFAULT 0,
    time_played_ms BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_grafana_role_playtime_realm_ts
    ON grafana_role_playtime_snapshot (realm, captured_at DESC);

CREATE TABLE IF NOT EXISTS grafana_skill_kdr_snapshot (
    id             BIGSERIAL     PRIMARY KEY,
    realm          INTEGER       NOT NULL,
    captured_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    skill_name     VARCHAR(200)  NOT NULL,
    kills          BIGINT        NOT NULL DEFAULT 0,
    deaths         BIGINT        NOT NULL DEFAULT 0,
    kdr            NUMERIC(10,2) NOT NULL DEFAULT 0,
    time_played_ms BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_grafana_skill_kdr_realm_ts
    ON grafana_skill_kdr_snapshot (realm, captured_at DESC);

-- ---------------------------------------------------------------------------
-- CONFIG SYNC TABLES  (upserted on every plugin start/reload)
-- Created by V20260530_2__Create_grafana_config_tables.sql
-- ---------------------------------------------------------------------------

-- Weapon damage / attack-speed config (one row per melee WeaponItem)
CREATE TABLE IF NOT EXISTS grafana_weapon_config (
    weapon_key        TEXT         NOT NULL PRIMARY KEY,
    weapon_name       TEXT         NOT NULL,
    weapon_type       TEXT         NOT NULL,   -- 'sword' | 'axe' | 'special'
    damage_base       NUMERIC(8,4) NOT NULL DEFAULT 0,
    damage_min        NUMERIC(8,4) NOT NULL DEFAULT 0,
    damage_max        NUMERIC(8,4) NOT NULL DEFAULT 0,
    attack_speed_base NUMERIC(8,4) NOT NULL DEFAULT 0,
    attack_speed_min  NUMERIC(8,4) NOT NULL DEFAULT 0,
    attack_speed_max  NUMERIC(8,4) NOT NULL DEFAULT 0,
    max_sockets       INTEGER      NOT NULL DEFAULT 4,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Armor health / durability config (one row per ArmorItem)
CREATE TABLE IF NOT EXISTS grafana_armor_config (
    item_key    TEXT         NOT NULL PRIMARY KEY,
    item_name   TEXT         NOT NULL,
    role_name   TEXT,                          -- NULL if not role-specific
    slot        TEXT,                          -- HELMET/CHESTPLATE/LEGGINGS/BOOTS
    health_base NUMERIC(8,4) NOT NULL DEFAULT 0,
    health_min  NUMERIC(8,4) NOT NULL DEFAULT 0,
    health_max  NUMERIC(8,4) NOT NULL DEFAULT 0,
    durability  INTEGER      NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Role base-health config (one row per Role enum value)
CREATE TABLE IF NOT EXISTS grafana_role_config (
    role_name   TEXT         NOT NULL PRIMARY KEY,
    base_health NUMERIC(8,4) NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Skill base config (cooldown, energy, max-level, etc.)
CREATE TABLE IF NOT EXISTS grafana_skill_config (
    skill_name                           TEXT         NOT NULL PRIMARY KEY,
    role_name                            TEXT,        -- NULL = global skill
    skill_type                           TEXT         NOT NULL,  -- SWORD/AXE/BOW/PASSIVE_A/PASSIVE_B/GLOBAL
    enabled                              BOOLEAN      NOT NULL DEFAULT TRUE,
    max_level                            INTEGER      NOT NULL DEFAULT 5,
    cooldown                             NUMERIC(8,4),           -- NULL if not CooldownSkill
    cooldown_decrease_per_level          NUMERIC(8,4),
    energy                               INTEGER,                -- NULL if not EnergySkill/EnergyChannelSkill
    energy_decrease_per_level            NUMERIC(8,4),
    energy_start_cost                    NUMERIC(8,4),           -- NULL if not ActiveToggleSkill
    energy_start_cost_decrease_per_level NUMERIC(8,4),
    base_charge                          NUMERIC(8,4),           -- NULL if not ChargeSkill
    charge_increase_per_level            NUMERIC(8,4),
    updated_at                           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Rune config params (key-value; one row per configurable field per rune)
CREATE TABLE IF NOT EXISTS grafana_rune_config (
    rune_key    TEXT        NOT NULL,
    rune_name   TEXT        NOT NULL,
    param_key   TEXT        NOT NULL,
    param_value TEXT        NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (rune_key, param_key)
);

-- Global energy system config (maxEnergy, energyPerSecond, nerfedEnergyPerSecond)
CREATE TABLE IF NOT EXISTS grafana_energy_config (
    param_key   TEXT          NOT NULL PRIMARY KEY,
    param_value NUMERIC(12,6) NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------------
-- VIEW: weapon × brutality-rune-count permutations with effective DPS
--   DEFAULT_DELAY  = 400 ms → base hits/sec = 2.5
--   hits_per_sec   = 2.5 × (1 + attack_speed)
--   brutality_bonus is read live from grafana_rune_config
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW grafana_weapon_damage_permutations AS
SELECT
    w.weapon_key,
    w.weapon_name,
    w.weapon_type,
    r.rune_count,

    w.weapon_name
        || CASE WHEN r.rune_count = 0 THEN ''
                ELSE ' +' || r.rune_count || 'B'
           END                                                           AS config_label,

    COALESCE(
        (SELECT param_value::NUMERIC
         FROM   grafana_rune_config
         WHERE  rune_key  = 'brutality'
           AND  param_key = 'damageIncrease'
         LIMIT  1),
        1.0)                                                             AS brutality_bonus,

    w.damage_min  + r.rune_count * COALESCE(
        (SELECT param_value::NUMERIC FROM grafana_rune_config
         WHERE rune_key = 'brutality' AND param_key = 'damageIncrease' LIMIT 1), 1.0)
                                                                         AS effective_min,
    w.damage_base + r.rune_count * COALESCE(
        (SELECT param_value::NUMERIC FROM grafana_rune_config
         WHERE rune_key = 'brutality' AND param_key = 'damageIncrease' LIMIT 1), 1.0)
                                                                         AS effective_base,
    w.damage_max  + r.rune_count * COALESCE(
        (SELECT param_value::NUMERIC FROM grafana_rune_config
         WHERE rune_key = 'brutality' AND param_key = 'damageIncrease' LIMIT 1), 1.0)
                                                                         AS effective_max,

    w.attack_speed_base,
    w.attack_speed_min,
    w.attack_speed_max,
    w.max_sockets,

    ROUND((w.damage_min  + r.rune_count * COALESCE(
        (SELECT param_value::NUMERIC FROM grafana_rune_config
         WHERE rune_key = 'brutality' AND param_key = 'damageIncrease' LIMIT 1), 1.0))
          * 2.5 * (1 + w.attack_speed_base), 3)                         AS dps_min,
    ROUND((w.damage_base + r.rune_count * COALESCE(
        (SELECT param_value::NUMERIC FROM grafana_rune_config
         WHERE rune_key = 'brutality' AND param_key = 'damageIncrease' LIMIT 1), 1.0))
          * 2.5 * (1 + w.attack_speed_base), 3)                         AS dps_base,
    ROUND((w.damage_max  + r.rune_count * COALESCE(
        (SELECT param_value::NUMERIC FROM grafana_rune_config
         WHERE rune_key = 'brutality' AND param_key = 'damageIncrease' LIMIT 1), 1.0))
          * 2.5 * (1 + w.attack_speed_base), 3)                         AS dps_max,

    ROUND((w.damage_min  + r.rune_count * COALESCE(
        (SELECT param_value::NUMERIC FROM grafana_rune_config
         WHERE rune_key = 'brutality' AND param_key = 'damageIncrease' LIMIT 1), 1.0))
          * 2.5 * (1 + w.attack_speed_min), 3)                          AS dps_worst,

    ROUND((w.damage_max  + r.rune_count * COALESCE(
        (SELECT param_value::NUMERIC FROM grafana_rune_config
         WHERE rune_key = 'brutality' AND param_key = 'damageIncrease' LIMIT 1), 1.0))
          * 2.5 * (1 + w.attack_speed_max), 3)                          AS dps_best

FROM  grafana_weapon_config w
CROSS JOIN (VALUES (0),(1),(2),(3),(4)) AS r(rune_count)
WHERE r.rune_count <= w.max_sockets;

-- ---------------------------------------------------------------------------
-- SUGGESTED GRAFANA QUERIES
-- ---------------------------------------------------------------------------
-- Hits-to-kill (vs a role, no armor):
--   SELECT w.config_label,
--          r.role_name,
--          CEIL(r.base_health / w.effective_base) AS hits_to_kill
--   FROM grafana_weapon_damage_permutations w
--   CROSS JOIN grafana_role_config r
--   ORDER BY r.role_name, w.weapon_type, w.rune_count;
--
-- Energy cost at level 1 per skill:
--   SELECT skill_name, role_name,
--          energy - energy_decrease_per_level AS energy_at_level_1
--   FROM grafana_skill_config
--   WHERE energy IS NOT NULL
--   ORDER BY energy_at_level_1 DESC;
--
-- Armor health per role (sum of all 4 pieces):
--   SELECT a.role_name,
--          SUM(a.health_base) AS total_armor_health_base
--   FROM grafana_armor_config a
--   WHERE a.role_name IS NOT NULL
--   GROUP BY a.role_name
--   ORDER BY total_armor_health_base DESC;
