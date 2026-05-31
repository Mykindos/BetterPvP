-- =============================================================================
-- Weapon Damage Config Seed
-- Source of truth: champions/src/main/resources/configs/items/weapon.yml
--
-- Re-run this file after changing weapon.yml to keep Grafana in sync.
-- max_sockets: update these if you change socket counts in-game.
-- brutality_bonus: default 1.0 from BrutalityRune.java — update if changed.
-- =============================================================================

INSERT INTO weapon_damage_config
    (weapon_key, weapon_name, weapon_type,
     damage_base, damage_min, damage_max,
     attack_speed_base, attack_speed_min, attack_speed_max,
     max_sockets, brutality_bonus)
VALUES

-- ── Standard weapon progression (swords) ────────────────────────────────────
('rustic_sword',   'Rustic Sword',   'sword',  4.0, 3.0, 5.0,  0.0, -0.25, 0.25, 0, 1.0),
('crude_sword',    'Crude Sword',    'sword',  5.0, 4.0, 6.0,  0.0, -0.25, 0.25, 0, 1.0),
('standard_sword', 'Standard Sword', 'sword',  6.0, 5.0, 7.0,  0.0, -0.25, 0.25, 1, 1.0),
('booster_sword',  'Booster Sword',  'sword',  6.0, 5.0, 7.0,  0.0, -0.25, 0.25, 2, 1.0),
('power_sword',    'Power Sword',    'sword',  6.5, 5.5, 7.5,  0.0, -0.25, 0.25, 2, 1.0),
('ancient_sword',  'Ancient Sword',  'sword',  7.0, 6.0, 8.0,  0.0, -0.25, 0.25, 2, 1.0),

-- ── Standard weapon progression (axes) ──────────────────────────────────────
('rustic_axe',     'Rustic Axe',     'axe',    4.0, 3.0, 5.0,  0.0, -0.25, 0.25, 0, 1.0),
('crude_axe',      'Crude Axe',      'axe',    5.0, 4.0, 6.0,  0.0, -0.25, 0.25, 0, 1.0),
('standard_axe',   'Standard Axe',   'axe',    6.0, 5.0, 7.0,  0.0, -0.25, 0.25, 1, 1.0),
('booster_axe',    'Booster Axe',    'axe',    6.0, 5.0, 7.0,  0.0, -0.25, 0.25, 2, 1.0),
('power_axe',      'Power Axe',      'axe',    6.5, 5.5, 7.5,  0.0, -0.25, 0.25, 2, 1.0),
('ancient_axe',    'Ancient Axe',    'axe',    7.0, 6.0, 8.0,  0.0, -0.25, 0.25, 2, 1.0),

-- ── Special / unique weapons ─────────────────────────────────────────────────
-- Hyper Axe: higher base attack speed (+0.3) — built-in attackSpeedPercentage
('hyper_axe',              'Hyper Axe',              'axe',     5.0, 4.0, 6.0,  0.3,  0.1,  0.5, 2, 1.0),
-- Mjolnir
('mjolnir',                'Mjolnir',                'special', 9.0, 8.0,10.0,  0.0, -0.25, 0.25, 2, 1.0),
-- Magnetic Maul
('magnetic_maul',          'Magnetic Maul',          'special', 7.0, 6.0, 8.0,  0.0, -0.25, 0.25, 2, 1.0),
-- Rake
('rake',                   'Rake',                   'special', 7.0, 6.0, 8.0,  0.0, -0.25, 0.25, 2, 1.0),
-- Alligator's Tooth
('alligators_tooth',       "Alligator's Tooth",      'special', 7.0, 6.0, 8.0,  0.0, -0.25, 0.25, 2, 1.0),
-- Giant's Broadsword
('giants_broadsword',      "Giant's Broadsword",     'special', 9.0, 8.0,10.0,  0.0, -0.25, 0.25, 2, 1.0),
-- Scythe of the Fallen Lord
('scythe_of_the_fallen_lord','Scythe of the Fallen Lord','special',9.0, 8.0,10.0, 0.0,-0.25, 0.25, 2, 1.0),
-- Thunderclap Aegis
('thunderclap_aegis',      'Thunderclap Aegis',      'special', 5.0, 4.0, 6.0,  0.0, -0.25, 0.25, 2, 1.0),
-- Wind Blade (note: config has damage.min > damage.base — unusual)
('wind_blade',             'Wind Blade',             'special', 6.0, 7.0, 8.0,  0.0, -0.25, 0.25, 2, 1.0),
-- Bloomrot
('bloomrot',               'Bloomrot',               'special', 6.0, 5.0, 7.0,  0.0, -0.25, 0.25, 2, 1.0),
-- Thornfang
('thornfang',              'Thornfang',              'special', 6.0, 5.0, 7.0,  0.0, -0.25, 0.25, 2, 1.0),
-- Hinokami Katana: high base attack speed (+0.5)
('hinokami_katana',        'Hinokami Katana',        'special', 6.0, 6.0, 8.0,  0.5,  0.25, 0.75, 2, 1.0),
-- Meridian Scepter: melee damage is 0 — all damage comes from abilities
('meridian_scepter',       'Meridian Scepter',       'special', 0.0, 0.0, 0.0,  0.0, -0.25, 0.25, 0, 1.0)

ON CONFLICT (weapon_key) DO UPDATE SET
    weapon_name       = EXCLUDED.weapon_name,
    weapon_type       = EXCLUDED.weapon_type,
    damage_base       = EXCLUDED.damage_base,
    damage_min        = EXCLUDED.damage_min,
    damage_max        = EXCLUDED.damage_max,
    attack_speed_base = EXCLUDED.attack_speed_base,
    attack_speed_min  = EXCLUDED.attack_speed_min,
    attack_speed_max  = EXCLUDED.attack_speed_max,
    max_sockets       = EXCLUDED.max_sockets,
    brutality_bonus   = EXCLUDED.brutality_bonus;

