CREATE TABLE IF NOT EXISTS zones
(
    id           BIGINT       PRIMARY KEY,
    zone_key     VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS zone_discoveries
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    client        BIGINT    NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    zone_id       BIGINT    NOT NULL REFERENCES zones (id)   ON DELETE CASCADE,
    discovered_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_zone_discovery UNIQUE (client, zone_id)
);

CREATE INDEX IF NOT EXISTS idx_zone_discoveries_client ON zone_discoveries (client);
