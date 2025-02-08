create table if not exists world_logs
(
    id       varchar(36)                           not null
        primary key,
    Server   varchar(255)                          not null,
    World    varchar(255)                          not null,
    BlockX   int                                   not null,
    BlockY   int                                   not null,
    BlockZ   int                                   not null,
    Action   varchar(255)                          not null,
    Material varchar(255)                          not null,
    Time     timestamp default current_timestamp() not null
);

create index world_logs_location_index
    on world_logs (Server, World, BlockX, BlockY, BlockZ, Time);

create index world_logs_time_index
    on world_logs (Server, Time);

create index world_logs_world_action_index
    on world_logs (Server, World, Action, Time);

create table if not exists world_logs_metadata
(
    LogId     varchar(36)  not null,
    MetaKey   varchar(255) not null,
    MetaValue varchar(255) not null,
    primary key (LogId, MetaKey),
    constraint world_logs_metadata_ibfk_1
        foreign key (LogId) references world_logs (id)
            on delete cascade
);

create index world_logs_metadata_key_value_index
    on world_logs_metadata (MetaKey, MetaValue);

create index world_logs_metadata_value_index
    on world_logs_metadata (MetaValue);


