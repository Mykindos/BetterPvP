create table ${tablePrefix}clients
(
    id   int auto_increment,
    UUID varchar(255) not null,
    Name varchar(255) not null,
    `Rank` varchar(64) not null default 'PLAYER',
    constraint clients_pk
        primary key (id)
);

create unique index ${tablePrefix}clients_UUID_uindex
    on ${tablePrefix}clients (UUID);