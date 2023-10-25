create table if not exists ${tablePrefix}killdeath_data
(
    Matchup varchar(255)  not null,
    Metric  varchar(255)  not null,
    Value   int default 0 null,
    constraint ${tablePrefix}killdeath_data_uk
    unique (Matchup, Metric)
);

create table if not exists ${tablePrefix}kills_data
(
    KillId varchar(36) not null primary key,
    KillerClass  varchar(20),
    VictimClass varchar(20),
    constraint killId_pk
        foreign key (KillId) references ${coreTablePrefix}kills (Id)
);

create table if not exists ${tablePrefix}kill_contributions
(
    ContributionId varchar(36) not null primary key,
    ContributorClass varchar(20),
    constraint contributionId_pk
        foreign key (ContributionId) references ${coreTablePrefix}kill_contributions (Id)
);

create table if not exists ${tablePrefix}ratings
(
    Gamer varchar(36) not null primary key,
    GoldRating int not null,
    NetheriteRating int not null,
    DiamondRating int not null,
    ChainmailRaiting int not null,
    IronRating int not null,
    LeatherRating int not null
);