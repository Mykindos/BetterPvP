create table if not exists ${tablePrefix}clients
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

create table if not exists ${tablePrefix}client_properties
(
    id       int          not null auto_increment,
    Client   varchar(255) not null,
    Property varchar(255) not null,
    Value    varchar(255) null,
    constraint ${tablePrefix}client_properties_pk
        primary key (id),
    constraint ${tablePrefix}client_properties_uk
        unique (Client, Property)
);

create table if not exists property_map
(
    Property varchar(255) not null,
    Type     varchar(255) not null,
    constraint property_map_pk
        primary key (Property, Type)
);