--Weapons/Runes/Legends
--Skills

CREATE TABLE IF NOT EXISTS champions_kills_build
(
    KillId         varchar(36) not null primary key,
    KillerSword    varchar(255) null,
    KillerAxe      varchar(255) null,
    KillerBow      varchar(255) null,
    KillerPassiveA varchar(255) null,
    KillerPassiveB varchar(255) null,
    KillerGlobal   varchar(255) null,
    VictimSword    varchar(255) null,
    VictimAxe      varchar(255) null,
    VictimBow      varchar(255) null,
    VictimPassiveA varchar(255) null,
    VictimPassiveB varchar(255) null,
    VictimGlobal   varchar(255) null
);

CREATE TABLE IF NOT EXISTS champions_kill_contributions_build
(
    ContributionId  varchar(36) not null primary key,
    Sword           varchar(255) null,
    Axe             varchar(255) null,
    Bow             varchar(255) null,
    PassiveA        varchar(255) null,
    PassiveB        varchar(255) null,
    Global          varchar(255) null
);
