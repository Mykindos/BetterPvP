create table if not exists clients
(
    id   int auto_increment,
    UUID varchar(36) not null,
    Name varchar(16) not null,
    `Rank` varchar(64) not null default 'PLAYER',
    constraint clients_pk
        primary key (id)
);

create unique index clients_UUID_uindex
    on clients (UUID);

INSERT IGNORE INTO clients (UUID, Name, `Rank`) VALUES ('e1f5d06b-685b-46a0-b22c-176d6aefffff', 'Mykindos', 'DEVELOPER');

create table if not exists armour
(
    Item      varchar(255) not null,
    Reduction double       null,
    constraint armour_pk
        primary key (Item)
);

INSERT IGNORE INTO armour (Item, Reduction) VALUES ('CHAINMAIL_BOOTS', 7);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('CHAINMAIL_CHESTPLATE', 18);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('CHAINMAIL_HELMET', 9);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('CHAINMAIL_LEGGINGS', 13);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('DIAMOND_BOOTS', 11);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('DIAMOND_CHESTPLATE', 20);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('DIAMOND_HELMET', 13);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('DIAMOND_LEGGINGS', 16);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('GOLDEN_BOOTS', 7);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('GOLDEN_CHESTPLATE', 18);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('GOLDEN_HELMET', 9);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('GOLDEN_LEGGINGS', 16);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('IRON_BOOTS', 11);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('IRON_CHESTPLATE', 20);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('IRON_HELMET', 13);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('IRON_LEGGINGS', 16);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('LEATHER_BOOTS', 6);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('LEATHER_CHESTPLATE', 13);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('LEATHER_HELMET', 7);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('LEATHER_LEGGINGS', 10);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('NETHERITE_BOOTS', 7);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('NETHERITE_CHESTPLATE', 18);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('NETHERITE_HELMET', 9);
INSERT IGNORE INTO armour (Item, Reduction) VALUES ('NETHERITE_LEGGINGS', 16);

create table if not exists client_properties
(
    Client   varchar(255) not null,
    Property varchar(255) not null,
    Value    varchar(255) null,
    primary key (Client, Property)
);

CREATE INDEX idx_client_properties_property ON client_properties (Property);

create table if not exists property_map
(
    Property varchar(255) not null,
    Type     varchar(255) not null,
    constraint property_map_pk
        primary key (Property, Type)
);

INSERT IGNORE INTO property_map VALUES ("CHAT_ENABLED", "boolean");
INSERT IGNORE INTO property_map VALUES ("COINS", "int");
INSERT IGNORE INTO property_map VALUES ("FRAGMENTS", "int");
INSERT IGNORE INTO property_map VALUES ("TIPS_ENABLED", "boolean");
INSERT IGNORE INTO property_map VALUES ("DROP_PROTECTION_ENABLED", "boolean");
INSERT IGNORE INTO property_map VALUES ("STAFF_CHAT", "boolean");
INSERT IGNORE INTO property_map VALUES ("BLOCKS_PLACED", "int");
INSERT IGNORE INTO property_map VALUES ("BLOCKS_BROKEN", "int");
INSERT IGNORE INTO property_map VALUES ("DAMAGE_DEALT", "double");
INSERT IGNORE INTO property_map VALUES ("DAMAGE_TAKEN", "double");
INSERT IGNORE INTO property_map VALUES ("COOLDOWN_DISPLAY", "boolean");
INSERT IGNORE INTO property_map VALUES ("BALANCE", "int");
INSERT IGNORE INTO property_map VALUES ("LAST_LOGIN", "long");
INSERT IGNORE INTO property_map VALUES ("TIME_CREATED", "long");
INSERT IGNORE INTO property_map VALUES ("EXPERIENCE", "double");
INSERT IGNORE INTO property_map VALUES ("LUNAR", "boolean");
INSERT IGNORE INTO property_map VALUES ("TIME_PLAYED", "long");
INSERT IGNORE INTO property_map VALUES ("COOLDOWN_SOUNDS_ENABLED", "boolean");

-- Profession properties
INSERT IGNORE INTO property_map VALUES ("TOTAL_LOGS_CHOPPED", "long");
INSERT IGNORE INTO property_map VALUES ("TOTAL_ORES_MINED", "long");
INSERT IGNORE INTO property_map VALUES ("TOTAL_FISH_CAUGHT", "long");
INSERT IGNORE INTO property_map VALUES ("TOTAL_WEIGHT_CAUGHT", "long");
INSERT IGNORE INTO property_map VALUES ("BIGGEST_FISH_CAUGHT", "long");

-- Clans Properties
INSERT IGNORE INTO property_map VALUES ("ALLY_CHAT", "boolean");
INSERT IGNORE INTO property_map VALUES ("CLAN_CHAT", "boolean");
INSERT IGNORE INTO property_map VALUES ("NO_DOMINANCE_COOLDOWN", "long");
INSERT IGNORE INTO property_map VALUES ("MAP_POINTS_OF_INTEREST", "boolean");
INSERT IGNORE INTO property_map VALUES ("MAP_PLAYER_NAMES", "boolean");
INSERT IGNORE INTO property_map VALUES ("SIDEBAR_ENABLED", "boolean");
INSERT IGNORE INTO property_map VALUES ("TERRITORY_POPUPS_ENABLED", "boolean");
INSERT IGNORE INTO property_map VALUES ("CLAN_MENU_ENABLED", "boolean");
INSERT IGNORE INTO property_map VALUES ("ENERGY", "int");
INSERT IGNORE INTO property_map VALUES ("LEVEL", "int");
INSERT IGNORE INTO property_map VALUES ("POINTS", "int");

-- Champions Properties
INSERT IGNORE INTO property_map VALUES ("ASSASSIN_EQUIPPED", "int");
INSERT IGNORE INTO property_map VALUES ("KNIGHT_EQUIPPED", "int");
INSERT IGNORE INTO property_map VALUES ("RANGER_EQUIPPED", "int");
INSERT IGNORE INTO property_map VALUES ("WARLOCK_EQUIPPED", "int");
INSERT IGNORE INTO property_map VALUES ("MAGE_EQUIPPED", "int");
INSERT IGNORE INTO property_map VALUES ("BRUTE_EQUIPPED", "int");
INSERT IGNORE INTO property_map VALUES ("SKILL_CHAT_PREVIEW", "boolean");
INSERT IGNORE INTO property_map VALUES ("SKILL_WEAPON_TOOLTIP", "boolean");
