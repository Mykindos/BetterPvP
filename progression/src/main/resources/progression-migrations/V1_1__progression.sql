create table if not exists ${tablePrefix}exp
(
    Gamer varchar(36) not null
        primary key,
    Fishing bigint default 0 not null,
    Mining bigint default 0 not null,
    Woodcutting bigint default 0 not null,
    Farming bigint default 0 not null
);

create table ${tablePrefix}fishing
(
    Gamer     varchar(36)             not null,
    Type      varchar(36)             not null,
    Weight    int                     not null,
    timestamp timestamp default now() not null
);
