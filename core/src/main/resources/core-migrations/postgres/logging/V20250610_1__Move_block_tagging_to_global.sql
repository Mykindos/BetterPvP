CREATE TABLE IF NOT EXISTS chunk_block_tagging
(
    realm        SMALLINT     NOT NULL,
    chunk        VARCHAR(255) NOT NULL,
    block_key    BIGINT       NOT NULL,
    tag          VARCHAR(255) NOT NULL,
    value        VARCHAR(255) NULL,
    last_updated BIGINT       NOT NULL,
    CONSTRAINT chunk_block_tagging_uk UNIQUE (realm, chunk, block_key, tag)
) PARTITION BY LIST (realm);
