create table if not exists ${tablePrefix}clients
(
    id   int auto_increment,
    UUID varchar(255) not null,
    Name varchar(255) not null,
    `Rank` varchar(64) not null default 'PLAYER',
    constraint clients_pk
        primary key (id)
);

create unique index ${tablePrefix}clients_UUID_uindex
    on ${tablePrefix}clients (UUID);

create table if not exists ${tablePrefix}armour
(
    Item      varchar(255) not null,
    Reduction double       null,
    constraint ${tablePrefix}armour_pk
        primary key (Item)
);

INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('CHAINMAIL_BOOTS', 8);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('CHAINMAIL_CHESTPLATE', 20);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('CHAINMAIL_HELMET', 8);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('CHAINMAIL_LEGGINGS', 16);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('DIAMOND_BOOTS', 12);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('DIAMOND_CHESTPLATE', 24);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('DIAMOND_HELMET', 12);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('DIAMOND_LEGGINGS', 19);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('GOLDEN_BOOTS', 9);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('GOLDEN_CHESTPLATE', 23);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('GOLDEN_HELMET', 8);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('GOLDEN_LEGGINGS', 19);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('IRON_BOOTS', 8);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('IRON_CHESTPLATE', 24);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('IRON_HELMET', 7);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('IRON_LEGGINGS', 22);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('LEATHER_BOOTS', 4);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('LEATHER_CHESTPLATE', 12);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('LEATHER_HELMET', 4);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('LEATHER_LEGGINGS', 8);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('NETHERITE_BOOTS', 8);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('NETHERITE_CHESTPLATE', 24);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('NETHERITE_HELMET', 8);
INSERT IGNORE INTO ${tablePrefix}armour (Item, Reduction) VALUES ('NETHERITE_LEGGINGS', 20);

create table if not exists ${tablePrefix}client_properties
(
    Client   varchar(255) not null,
    Property varchar(255) not null,
    Value    varchar(255) null,
    primary key (Client, Property)
);

create table if not exists property_map
(
    Property varchar(255) not null,
    Type     varchar(255) not null,
    constraint property_map_pk
        primary key (Property, Type)
);

INSERT IGNORE INTO property_map VALUES ("CHAT_ENABLED", "java.lang.Boolean");