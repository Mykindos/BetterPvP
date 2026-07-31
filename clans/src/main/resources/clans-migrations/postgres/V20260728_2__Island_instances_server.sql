ALTER TABLE island_instances
    ADD COLUMN IF NOT EXISTS server VARCHAR(64) NOT NULL DEFAULT 'unknown';

CREATE INDEX IF NOT EXISTS idx_island_instances_realm_server ON island_instances (realm, server);
