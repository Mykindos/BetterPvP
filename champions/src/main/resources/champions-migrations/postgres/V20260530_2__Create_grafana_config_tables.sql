-- =============================================================================
-- Grafana config tables for Champions balance / TTK dashboards.
-- Rows are UPSERTED from the Champions plugin (GrafanaConfigSyncService)
-- on every server start / /champions reload.
-- YAML files remain the source of truth; the DB is kept in sync.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Weapon damage / attack-speed config  (one row per melee WeaponItem)
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- 2. Armor health / durability config  (one row per ArmorItem)
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- 3. Role base-health config  (one row per Role enum value)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS grafana_role_config (
    role_name   TEXT         NOT NULL PRIMARY KEY,
    base_health NUMERIC(8,4) NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------------
-- 4. Skill base config  (one row per Skill)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS grafana_skill_config (
    skill_name                           TEXT         NOT NULL PRIMARY KEY,
    role_name                            TEXT,        -- NULL = global skill
    skill_type                           TEXT         NOT NULL,  -- SWORD/AXE/BOW/PASSIVE_A/PASSIVE_B/GLOBAL
    enabled                              BOOLEAN      NOT NULL DEFAULT TRUE,
    max_level                            INTEGER      NOT NULL DEFAULT 5,
    -- CooldownSkill fields (NULL when not applicable)
    cooldown                             NUMERIC(8,4),
    cooldown_decrease_per_level          NUMERIC(8,4),
    -- EnergySkill / EnergyChannelSkill fields
    energy                               INTEGER,
    energy_decrease_per_level            NUMERIC(8,4),
    -- ActiveToggleSkill fields
    energy_start_cost                    NUMERIC(8,4),
    energy_start_cost_decrease_per_level NUMERIC(8,4),
    -- ChargeSkill fields
    base_charge                          NUMERIC(8,4),
    charge_increase_per_level            NUMERIC(8,4),
    updated_at                           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------------
-- 5. Rune config params  (key-value; one row per configurable field per rune)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS grafana_rune_config (
    rune_key    TEXT        NOT NULL,
    rune_name   TEXT        NOT NULL,
    param_key   TEXT        NOT NULL,
    param_value TEXT        NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (rune_key, param_key)
);

-- ---------------------------------------------------------------------------
-- 6. Energy system config  (global values from EnergyService)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS grafana_energy_config (
    param_key   TEXT          NOT NULL PRIMARY KEY,
    param_value NUMERIC(12,6) NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------------
-- 7. View: weapon × brutality-rune-count permutations with effective DPS
--    DEFAULT_DELAY = 400 ms  →  base hits/sec = 2.5
--    effective_delay = 400 / (1 + attack_speed)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW grafana_weapon_damage_permutations AS
SELECT
    w.weapon_key,
    w.weapon_name,
    w.weapon_type,
    r.rune_count,

    -- Label for Grafana chart axes
    w.weapon_name
        || CASE WHEN r.rune_count = 0 THEN ''
                ELSE ' +' || r.rune_count || 'B'
           END                                                           AS config_label,

    -- Brutality bonus pulled live from the rune config table
    COALESCE(
        (SELECT param_value::NUMERIC
         FROM   grafana_rune_config
         WHERE  rune_key  = 'brutality'
           AND  param_key = 'damageIncrease'
         LIMIT  1),
        1.0)                                                             AS brutality_bonus,

    -- Effective damage at this rune count
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

    -- DPS at base attack speed
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

    -- Worst-case DPS (min damage + slowest speed)
    ROUND((w.damage_min  + r.rune_count * COALESCE(
        (SELECT param_value::NUMERIC FROM grafana_rune_config
         WHERE rune_key = 'brutality' AND param_key = 'damageIncrease' LIMIT 1), 1.0))
          * 2.5 * (1 + w.attack_speed_min), 3)                          AS dps_worst,

    -- Best-case DPS (max damage + fastest speed)
    ROUND((w.damage_max  + r.rune_count * COALESCE(
        (SELECT param_value::NUMERIC FROM grafana_rune_config
         WHERE rune_key = 'brutality' AND param_key = 'damageIncrease' LIMIT 1), 1.0))
          * 2.5 * (1 + w.attack_speed_max), 3)                          AS dps_best

FROM  grafana_weapon_config w
CROSS JOIN (VALUES (0),(1),(2),(3),(4)) AS r(rune_count)
WHERE r.rune_count <= w.max_sockets;

