CREATE TABLE IF NOT EXISTS smart_block_data
(
    server         varchar(128)                          not null,
    chunk_key      bigint                                not null,
    block_key      int                                   not null,
    block_type     varchar(255)                          not null,
    data_type_class varchar(255)                         not null,
    data           longblob                              not null,
    last_updated   timestamp default current_timestamp() not null,
    primary key (server, chunk_key, block_key),
    index idx_chunk_serializer (server, chunk_key),
    index idx_chunk_type (server, chunk_key, block_type),
    index idx_last_updated (last_updated)
); 