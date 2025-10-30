CREATE TABLE IF NOT EXISTS chunk_block_tagging
(
    realm        SMALLINT     NOT NULL,
    chunk        VARCHAR(255) NOT NULL,
    block_key    BIGINT       NOT NULL,
    tag          VARCHAR(255) NOT NULL,
    value        VARCHAR(255) NULL,
    last_updated TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT chunk_block_tagging_uk UNIQUE (realm, chunk, block_key, tag)
) PARTITION BY LIST (realm);

CREATE TABLE IF NOT EXISTS chunk_block_tagging_1 PARTITION OF chunk_block_tagging FOR VALUES IN (1);