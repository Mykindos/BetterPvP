CREATE TABLE IF NOT EXISTS punishments
(
    id            SERIAL       PRIMARY KEY,
    client        BIGINT       NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    type          VARCHAR(255) NOT NULL,
    rule          VARCHAR(255) NOT NULL,
    apply_time    BIGINT       NOT NULL,
    expiry_time   BIGINT       NOT NULL,
    reason        VARCHAR(255) NULL,
    punisher      VARCHAR(36)  NULL,
    revoker       VARCHAR(36)  NULL,
    revoke_type   VARCHAR(255) NULL,
    revoke_time   BIGINT       NOT NULL DEFAULT -1,
    revoke_reason VARCHAR(255) NULL
);

CREATE INDEX IF NOT EXISTS idx_punishments_client ON punishments (client);