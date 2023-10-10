create table if not exists ${tablePrefix}damagevalues
(
    Material varchar(255) not null,
    Damage   double       null,
    constraint ${tablePrefix}damagevalues_pk
        primary key (Material)
);

INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("DIAMOND_SWORD", 6);
INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("DIAMOND_AXE", 6);
INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("GOLDEN_SWORD", 5);
INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("GOLDEN_AXE", 5);
INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("NETHERITE_SWORD", 6);
INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("NETHERITE_AXE", 6);
INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("IRON_SWORD", 5);
INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("IRON_AXE", 5);
INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("STONE_SWORD", 3);
INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("WOODEN_SWORD", 2);
INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("STONE_AXE", 2);
INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("WOODEN_AXE", 1);
INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("TRIDENT", 2);
INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("DIAMOND_SHOVEL", 2);
INSERT IGNORE INTO ${tableprefix}damagevalues VALUES ("IRON_SHOVEL", 1);
