create table if not exists ${tablePrefix}gamers
(
    id   int auto_increment,
    UUID varchar(255) not null,
    constraint gamers_pk
        primary key (id)
);

create unique index ${tablePrefix}gamers_UUID_uindex
    on ${tablePrefix}gamers (UUID);

create table if not exists ${tablePrefix}gamer_properties
(
    id       int          not null auto_increment,
    Gamer    varchar(255) not null,
    Property varchar(255) not null,
    Value    varchar(255) null,
    constraint ${tablePrefix}gamer_properties_pk
        primary key (id),
    constraint ${tablePrefix}gamer_properties_uk
        unique (Gamer, Property)
);

create table if not exists ${tablePrefix}clans
(
    id        int         auto_increment,
    Name      varchar(32) not null,
    Created   TIMESTAMP   null default CURRENT_TIMESTAMP,
    Home      varchar(64) null,
    Admin     tinyint     null default 0,
    Safe      tinyint     null default 0,
    Energy    int         null default 0,
    Points    int         null default 0,
    Cooldown  bigint      null default 0,
    Level     int         null default 1,
    LastLogin TIMESTAMP   null default CURRENT_TIMESTAMP,
    constraint clans_pk
        primary key (id)
);

create unique index ${tablePrefix}clans_Name_uindex
    on ${tablePrefix}clans (Name);

create table if not exists ${tablePrefix}clan_territory
(
    id        int         auto_increment not null,
    Clan      int         not null,
    Chunk     varchar(64) not null,
    constraint clan_territory_pk
        primary key (id)
);

create unique index ${tablePrefix}clans_territory_Clan_Chunk_uindex
    on ${tablePrefix}clan_territory (Clan, Chunk);

create table if not exists ${tablePrefix}clan_members
(
    id        int         auto_increment not null,
    Clan      int         not null,
    Member    varchar(64) not null,
    `Rank`    varchar(64) not null default 'RECRUIT',
    constraint clan_members_pk
        primary key (id)
);

create unique index ${tablePrefix}clans_members_Clan_Member_uindex
    on ${tablePrefix}clan_members (Clan, Member);

create table if not exists ${tablePrefix}clan_alliances
(
    id        int         auto_increment not null,
    Clan      int         not null,
    AllyClan  int         not null,
    Trusted   tinyint     default 0,
    constraint clan_alliances_pk
        primary key (id)
);

create unique index ${tablePrefix}clans_alliances_Clan_AllyClan_uindex
    on ${tablePrefix}clan_alliances (Clan, AllyClan);

create table if not exists ${tablePrefix}clan_enemies
(
    id        int         auto_increment not null,
    Clan      int         not null,
    EnemyClan  int        not null,
    Dominance  tinyint    default 0,
    constraint clan_enemies_pk
        primary key (id)
);

create unique index ${tablePrefix}clans_enemies_Clan_EnemyClan_uindex
    on ${tablePrefix}clan_enemies (Clan, EnemyClan);

# noinspection SqlResolve
INSERT IGNORE INTO property_map VALUES ("COINS", "java.lang.Integer");
# noinspection SqlResolve
INSERT IGNORE INTO property_map VALUES ("FRAGMENTS", "java.lang.Integer");
# noinspection SqlResolve
INSERT IGNORE INTO property_map VALUES ("CLANS_SIDEBAR_ENABLED", "java.lang.Boolean");
# noinspection SqlResolve
INSERT IGNORE INTO property_map VALUES ("ALLY_CHAT", "java.lang.Boolean");
# noinspection SqlResolve
INSERT IGNORE INTO property_map VALUES ("CLAN_CHAT", "java.lang.Boolean");
# noinspection SqlResolve
INSERT IGNORE INTO property_map VALUES ("STAFF_CHAT", "java.lang.Boolean");
