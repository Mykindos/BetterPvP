CREATE TABLE IF NOT EXISTS smart_block_data (
    realm            SMALLINT     NOT NULL,
    world            VARCHAR(255) NOT NULL,
    chunk_key        BIGINT       NOT NULL,
    block_key        BIGINT       NOT NULL,
    block_type       VARCHAR(255) NOT NULL,
    data_type_class  VARCHAR(255) NOT NULL,
    data             BYTEA        NOT NULL,
    PRIMARY KEY (realm, world, chunk_key, block_key)
);