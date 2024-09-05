create table if not exists clans
(
    id        varchar(36) not null,
    Name      varchar(32) not null,
    Home      varchar(64) null,
    Admin     tinyint     null default 0,
    Safe      tinyint     null default 0,
    Banner    TEXT        null,
    Vault     TEXT        null default null,
    Mailbox   TEXT        null default null,
    constraint clans_pk
        primary key (id)
);

alter table clans add index (Name);

create table if not exists clan_territory
(
    id        int         auto_increment not null,
    Clan      varchar(36) not null,
    Chunk     varchar(64) not null,
    constraint clan_territory_pk
        primary key (id)
);

create unique index clans_territory_Clan_Chunk_uindex
    on clan_territory (Clan, Chunk);

create table if not exists clan_members
(
    id        int         auto_increment not null,
    Clan      varchar(36) not null,
    Member    varchar(64) not null,
    `Rank`    varchar(64) not null default 'RECRUIT',
    constraint clan_members_pk
        primary key (id)
);

create unique index clan_members_Clan_Member_uindex
    on clan_members (Clan, Member);

create table if not exists clan_alliances
(
    id        int         auto_increment not null,
    Clan      varchar(36) not null,
    AllyClan  varchar(36) not null,
    Trusted   tinyint     default 0,
    constraint clan_alliances_pk
        primary key (id)
);

create unique index clan_alliances_Clan_AllyClan_uindex
    on clan_alliances (Clan, AllyClan);

create table if not exists clan_enemies
(
    id         int         auto_increment not null,
    Clan       varchar(36) not null,
    EnemyClan  varchar(36) not null,
    Dominance  tinyint    default 0,
    constraint clan_enemies_pk
        primary key (id)
);

create unique index clan_enemies_Clan_EnemyClan_uindex
    on clan_enemies (Clan, EnemyClan);

create table if not exists clans_dominance_scale
(
    ClanSize  int    not null,
    Dominance double not null,
    constraint clans_dominance_scale_pk
        primary key (ClanSize)
);

INSERT IGNORE INTO clans_dominance_scale VALUES (0, 3.5);
INSERT IGNORE INTO clans_dominance_scale VALUES (1, 3.5);
INSERT IGNORE INTO clans_dominance_scale VALUES (2, 3.5);
INSERT IGNORE INTO clans_dominance_scale VALUES (3, 4);
INSERT IGNORE INTO clans_dominance_scale VALUES (4, 4);
INSERT IGNORE INTO clans_dominance_scale VALUES (5, 4.5);
INSERT IGNORE INTO clans_dominance_scale VALUES (6, 4.5);
INSERT IGNORE INTO clans_dominance_scale VALUES (7, 5);
INSERT IGNORE INTO clans_dominance_scale VALUES (8, 5);

create table if not exists clan_insurance
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

create table if not exists clan_properties
(
    Clan     varchar(36)  not null,
    Property varchar(255) not null,
    Value    varchar(255) null,
    primary key (Clan, Property)
);