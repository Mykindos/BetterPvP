create table if not exists ${tablePrefix}exp
(
    gamer varchar(36) not null
        primary key,
    fishing bigint default 0 not null,
    mining bigint default 0 not null,
    woodcutting bigint default 0 not null,
    farming bigint default 0 not null
);

create table ${tablePrefix}fishing
(
    gamer     varchar(36)             not null,
    type      varchar(36)             not null,
    weight    int                     not null,
    timestamp timestamp default now() not null
);
