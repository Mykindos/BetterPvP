create table if not exists champions_damagevalues
(
    Material varchar(255) not null,
    Damage   double       null,
    constraint champions_damagevalues_pk
        primary key (Material)
);

INSERT IGNORE INTO champions_damagevalues VALUES ("DIAMOND_SWORD", 7);
INSERT IGNORE INTO champions_damagevalues VALUES ("DIAMOND_AXE", 7);
INSERT IGNORE INTO champions_damagevalues VALUES ("GOLDEN_SWORD", 6);
INSERT IGNORE INTO champions_damagevalues VALUES ("GOLDEN_AXE", 6);
INSERT IGNORE INTO champions_damagevalues VALUES ("NETHERITE_SWORD", 7);
INSERT IGNORE INTO champions_damagevalues VALUES ("NETHERITE_AXE", 7);
INSERT IGNORE INTO champions_damagevalues VALUES ("IRON_SWORD", 6);
INSERT IGNORE INTO champions_damagevalues VALUES ("IRON_AXE", 6);
INSERT IGNORE INTO champions_damagevalues VALUES ("STONE_SWORD", 5);
INSERT IGNORE INTO champions_damagevalues VALUES ("WOODEN_SWORD", 3);
INSERT IGNORE INTO champions_damagevalues VALUES ("STONE_AXE", 5);
INSERT IGNORE INTO champions_damagevalues VALUES ("WOODEN_AXE", 1);
INSERT IGNORE INTO champions_damagevalues VALUES ("TRIDENT", 2);
INSERT IGNORE INTO champions_damagevalues VALUES ("DIAMOND_SHOVEL", 2);
INSERT IGNORE INTO champions_damagevalues VALUES ("IRON_SHOVEL", 1);
