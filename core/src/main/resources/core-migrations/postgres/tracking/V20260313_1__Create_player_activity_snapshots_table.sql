CREATE TABLE IF NOT EXISTS player_activity_snapshots
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    world           VARCHAR(64)       NOT NULL,
    chunk_x         INT               NOT NULL,
    chunk_z         INT               NOT NULL,
    heat_value      DOUBLE PRECISION  NOT NULL,
    peak_heat       DOUBLE PRECISION  NOT NULL,
    combat_events   INT               NOT NULL DEFAULT 0,
    current_players INT               NOT NULL DEFAULT 0,
    recorded_at     BIGINT            NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pas_world_time ON player_activity_snapshots (world, recorded_at);
CREATE INDEX IF NOT EXISTS idx_pas_chunk      ON player_activity_snapshots (world, chunk_x, chunk_z);
