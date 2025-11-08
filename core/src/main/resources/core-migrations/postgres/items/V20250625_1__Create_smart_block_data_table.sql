CREATE TABLE IF NOT EXISTS smart_block_data (
    realm            SMALLINT     NOT NULL,
    chunk_key        BIGINT       NOT NULL,
    block_key        INTEGER      NOT NULL,
    block_type       VARCHAR(255) NOT NULL,
    data_type_class  VARCHAR(255) NOT NULL,
    data             BYTEA        NOT NULL,
    last_updated     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (realm, chunk_key, block_key)
);

CREATE INDEX idx_chunk_serializer ON smart_block_data (realm, chunk_key);
CREATE INDEX idx_chunk_type ON smart_block_data (realm, chunk_key, block_type);
CREATE INDEX idx_last_updated ON smart_block_data (last_updated);
