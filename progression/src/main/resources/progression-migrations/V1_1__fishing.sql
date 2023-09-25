create table clans_fishing
(
    gamer     varchar(36)             not null,
    type      varchar(36)             not null,
    weight    int                     not null,
    timestamp timestamp default now() not null
);
