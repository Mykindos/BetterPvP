create table ${tablePrefix}gamers
(
    id   int auto_increment,
    UUID varchar(255) not null,
    constraint gamers_pk
        primary key (id)
);

create unique index ${tablePrefix}gamers_UUID_uindex
    on ${tablePrefix}gamers (UUID);

create table ${tablePrefix}clans
(
    Name      varchar(14) not null
        primary key,
    Created   mediumtext  null,
    Leader    varchar(64) null,
    Home      varchar(64) null,
    Territory blob        null,
    Admin     tinyint     null,
    Safe      tinyint     null,
    LastLogin bigint      null,
    Energy    int         null,
    Points    int         null,
    Cooldown  mediumtext  null,
    Level     int         null
);

