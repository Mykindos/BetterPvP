
CREATE TABLE IF NOT EXISTS champions_damagevalues
(
    material VARCHAR(255) NOT NULL,
    damage   DOUBLE PRECISION NULL,
    CONSTRAINT champions_damagevalues_pk
        PRIMARY KEY (material)
);

INSERT INTO champions_damagevalues (material, damage)
VALUES ('NETHERITE_SWORD', 6),
       ('NETHERITE_AXE', 6),
       ('DIAMOND_SWORD', 6),
       ('DIAMOND_AXE', 6),
       ('GOLDEN_SWORD', 5),
       ('GOLDEN_AXE', 5),
       ('IRON_SWORD', 5),
       ('IRON_AXE', 5),
       ('STONE_SWORD', 4),
       ('STONE_AXE', 4),
       ('WOODEN_SWORD', 3),
       ('WOODEN_AXE', 3),
       ('TRIDENT', 2),
       ('DIAMOND_SHOVEL', 2),
       ('IRON_SHOVEL', 1)
ON CONFLICT (material) DO NOTHING;