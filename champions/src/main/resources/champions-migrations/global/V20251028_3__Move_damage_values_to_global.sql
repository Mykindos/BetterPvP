create table if not exists champions_damagevalues
(
    Material varchar(255) not null,
    Damage   double       null,
    constraint champions_damagevalues_pk
        primary key (Material)
);

INSERT IGNORE INTO champions_damagevalues (Material, Damage)
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
       ('IRON_SHOVEL', 1);