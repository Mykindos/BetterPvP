CREATE TABLE IF NOT EXISTS island_instances
(
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    realm      SMALLINT     NOT NULL,
    template   VARCHAR(64)  NOT NULL,
    world      VARCHAR(255) NOT NULL,
    state      VARCHAR(32)  NOT NULL DEFAULT 'PROVISIONING',
    created_at BIGINT       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_island_instances_realm ON island_instances (realm);
