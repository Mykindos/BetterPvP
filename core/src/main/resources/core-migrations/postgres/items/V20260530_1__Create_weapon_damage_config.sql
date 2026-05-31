-- Weapon damage configuration table, kept in sync by the server plugin (WeaponItem.reload()).
-- Source of truth is configs/weapons.yml; this table exists purely for Grafana dashboards.

CREATE TABLE IF NOT EXISTS weapon_damage_config (
    weapon_key          VARCHAR(100)  PRIMARY KEY,
    weapon_name         VARCHAR(200)  NOT NULL,
    weapon_type         VARCHAR(50)   NOT NULL DEFAULT 'sword',
    damage_base         NUMERIC(10,4) NOT NULL DEFAULT 1.0,
    damage_min          NUMERIC(10,4) NOT NULL DEFAULT 0.0,
    damage_max          NUMERIC(10,4) NOT NULL DEFAULT 2.0,
    attack_speed_base   NUMERIC(10,4) NOT NULL DEFAULT 0.0,
    attack_speed_min    NUMERIC(10,4) NOT NULL DEFAULT -0.25,
    attack_speed_max    NUMERIC(10,4) NOT NULL DEFAULT 0.25,
    max_sockets         INTEGER       NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- View: all stat permutations (min/max rolls) for every possible socket count per weapon.
-- TTK is estimated against a 20 HP unarmoured target at Minecraft's base 4 attacks/sec.
CREATE OR REPLACE VIEW weapon_damage_permutations AS
WITH socket_counts AS (
    SELECT weapon_key, generate_series(0, max_sockets) AS n_sockets
    FROM weapon_damage_config
)
SELECT
    wdc.weapon_key,
    wdc.weapon_name,
    wdc.weapon_type,
    sc.n_sockets,
    -- Damage rolled range with n sockets
    ROUND(wdc.damage_base + sc.n_sockets * wdc.damage_min,         4) AS damage_rolled_min,
    ROUND(wdc.damage_base + sc.n_sockets * wdc.damage_max,         4) AS damage_rolled_max,
    -- Attack speed rolled range with n sockets
    ROUND(wdc.attack_speed_base + sc.n_sockets * wdc.attack_speed_min, 4) AS attack_speed_rolled_min,
    ROUND(wdc.attack_speed_base + sc.n_sockets * wdc.attack_speed_max, 4) AS attack_speed_rolled_max,
    -- TTK (seconds) at best rolls vs 20 HP naked target, base 4 hits/sec
    ROUND(20.0 / NULLIF(
        (wdc.damage_base + sc.n_sockets * wdc.damage_max)
        * (4.0 + wdc.attack_speed_base + sc.n_sockets * wdc.attack_speed_max),
        0), 3) AS ttk_best_secs,
    -- TTK (seconds) at worst rolls
    ROUND(20.0 / NULLIF(
        (wdc.damage_base + sc.n_sockets * wdc.damage_min)
        * (4.0 + wdc.attack_speed_base + sc.n_sockets * wdc.attack_speed_min),
        0), 3) AS ttk_worst_secs
FROM weapon_damage_config wdc
JOIN socket_counts sc ON sc.weapon_key = wdc.weapon_key
ORDER BY wdc.weapon_key, sc.n_sockets;

