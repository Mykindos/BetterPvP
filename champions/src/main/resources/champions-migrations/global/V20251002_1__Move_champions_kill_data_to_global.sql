create table if not exists champions_killdeath_data
(
    Matchup varchar(255)  not null,
    Metric  varchar(255)  not null,
    Value   int default 0 null,
    constraint champions_killdeath_data_uk
        unique (Matchup, Metric)
);

create table if not exists champions_kills
(
    KillId      varchar(36) not null primary key,
    KillerClass varchar(20) default '',
    VictimClass varchar(20) default ''
);

create table if not exists champions_kill_contributions
(
    ContributionId   varchar(36) not null primary key,
    ContributorClass varchar(20) default ''
);

create table if not exists champions_combat_stats
(
    Gamer             varchar(36) not null,
    Server            varchar(64) not null,
    Season            varchar(64) not null,
    Class             varchar(20) default '',
    Rating            int         not null,
    Killstreak        int         not null default 0,
    HighestKillstreak int         not null default 0,
    Valid             tinyint     not null default 1
);

alter table champions_combat_stats
    add constraint champions_combat_stats_pk
        primary key (Gamer, Server, Season, Class);

