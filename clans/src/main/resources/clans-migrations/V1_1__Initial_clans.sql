create table if not exists ${tablePrefix}clans
(
    id        varchar(36) not null,
    Name      varchar(32) not null,
    Home      varchar(64) null,
    Admin     tinyint     null default 0,
    Safe      tinyint     null default 0,
    Banner    TEXT        null default '',
    constraint clans_pk
        primary key (id)
);

create unique index ${tablePrefix}clans_Name_uindex
    on ${tablePrefix}clans (Name);

create table if not exists ${tablePrefix}clan_territory
(
    id        int         auto_increment not null,
    Clan      varchar(36) not null,
    Chunk     varchar(64) not null,
    constraint clan_territory_pk
        primary key (id)
);

create unique index ${tablePrefix}clans_territory_Clan_Chunk_uindex
    on ${tablePrefix}clan_territory (Clan, Chunk);

create table if not exists ${tablePrefix}clan_members
(
    id        int         auto_increment not null,
    Clan      varchar(36) not null,
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
    Clan      varchar(36) not null,
    AllyClan  varchar(36) not null,
    Trusted   tinyint     default 0,
    constraint clan_alliances_pk
        primary key (id)
);

create unique index ${tablePrefix}clans_alliances_Clan_AllyClan_uindex
    on ${tablePrefix}clan_alliances (Clan, AllyClan);

create table if not exists ${tablePrefix}clan_enemies
(
    id         int         auto_increment not null,
    Clan       varchar(36) not null,
    EnemyClan  varchar(36) not null,
    Dominance  tinyint    default 0,
    constraint clan_enemies_pk
        primary key (id)
);

create unique index ${tablePrefix}clans_enemies_Clan_EnemyClan_uindex
    on ${tablePrefix}clan_enemies (Clan, EnemyClan);


create table if not exists ${tablePrefix}dominance_scale
(
    ClanSize  int not null,
    Dominance int not null,
    constraint ${tablePrefix}dominance_scale_pk
        primary key (ClanSize)
);

INSERT IGNORE INTO ${tablePrefix}dominance_scale VALUES (1, 4);
INSERT IGNORE INTO ${tablePrefix}dominance_scale VALUES (2, 4);
INSERT IGNORE INTO ${tablePrefix}dominance_scale VALUES (3, 5);
INSERT IGNORE INTO ${tablePrefix}dominance_scale VALUES (4, 5);
INSERT IGNORE INTO ${tablePrefix}dominance_scale VALUES (5, 6);
INSERT IGNORE INTO ${tablePrefix}dominance_scale VALUES (6, 6);

create table if not exists ${tablePrefix}insurance
(
    Clan          varchar(36) not null,
    InsuranceType varchar(255)  not null,
    Material      varchar(255)  not null,
    Data          varchar(1000) null,
    Time          bigint        not null,
    X             int           not null,
    Y             int           not null,
    Z             int           not null
);

create table if not exists ${tablePrefix}clan_properties
(
    Clan     varchar(36)  not null,
    Property varchar(255) not null,
    Value    varchar(255) null,
    primary key (Clan, Property)
);

INSERT IGNORE INTO property_map VALUES ("ENERGY", "int");
INSERT IGNORE INTO property_map VALUES ("LEVEL", "int");
INSERT IGNORE INTO property_map VALUES ("POINTS", "int");
INSERT IGNORE INTO property_map VALUES ("BALANCE", "int");
INSERT IGNORE INTO property_map VALUES ("LAST_LOGIN", "long");
INSERT IGNORE INTO property_map VALUES ("NO_DOMINANCE_COOLDOWN", "long");
INSERT IGNORE INTO property_map VALUES ("LAST_TNTED", "long");
INSERT IGNORE INTO property_map VALUES ("TIME_CREATED", "long");