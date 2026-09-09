CREATE TABLE IF NOT EXISTS site_instances
(
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    realm      SMALLINT     NOT NULL,
    server     VARCHAR(64)  NOT NULL,
    site       VARCHAR(64)  NOT NULL,
    owner      BIGINT       NOT NULL DEFAULT 0,
    world      VARCHAR(255) NOT NULL,
    state      VARCHAR(32)  NOT NULL DEFAULT 'PROVISIONING',
    created_at BIGINT       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_site_instances_realm_server ON site_instances (realm, server);
CREATE INDEX IF NOT EXISTS idx_site_instances_site_owner ON site_instances (site, owner);
