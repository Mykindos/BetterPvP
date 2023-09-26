create table ${tablePrefix}exp
(
    gamer       varchar(36) not null,
    fishing     bigint      not null,
    mining      bigint      not null,
    woodcutting bigint      null,
    farming     bigint      not null,
    constraint progression_exp_pk
        primary key (gamer)
);

create table ${tablePrefix}fishing
(
    gamer     varchar(36)             not null,
    type      varchar(36)             not null,
    weight    int                     not null,
    timestamp timestamp default now() not null
);
