create table if not exists chunk_block_tagging
(
    Server      varchar(128)                          not null,
    Season      varchar(128)                          not null,
    Chunk       varchar(255)                          not null,
    BlockKey    bigint                                not null,
    Tag         varchar(255)                          not null,
    Value       varchar(255)                          null,
    LastUpdated timestamp default current_timestamp() null,
    unique key (Server, Season, Chunk, BlockKey, Tag)
);
