-- Where a player was when they last left, and the last site that will have them back. One row per kind so that a
-- third kind costs a value rather than a column, and clearing one is a delete.
CREATE TABLE IF NOT EXISTS site_residency
(
    client     BIGINT       NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    kind       VARCHAR(16)  NOT NULL,
    site       VARCHAR(64)  NOT NULL,
    owner      BIGINT       NOT NULL DEFAULT 0,
    instance   VARCHAR(36)  NOT NULL,
    server     VARCHAR(64)  NOT NULL,
    location   VARCHAR(255) NOT NULL,
    updated_at BIGINT       NOT NULL,
    PRIMARY KEY (client, kind)
);
