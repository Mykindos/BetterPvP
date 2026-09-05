-- Per-player depletion of a personal resource node.
--
-- A node whose depletion is per player keeps no world state to re-read at load: the world block never changes, so
-- there is nothing to scan and nothing to derive. Without a record here the mine would refill for a player the moment
-- they relogged, which would make the respawn timer skippable and therefore not a timer at all.
--
-- This lives in the database rather than in a server-local cache because spawn may be sharded: a player who hops
-- shards must find the mine exactly as they left it, and a file in one shard's data folder is invisible to the other.
--
-- One row per depleted point, rather than one blob per player, so a respawn is a single DELETE and two shards
-- touching different blocks never fight over the same row.
CREATE TABLE IF NOT EXISTS personal_mine_points
(
    client   BIGINT      NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    world    VARCHAR(64) NOT NULL,
    node     VARCHAR(64) NOT NULL,
    x        INT         NOT NULL,
    y        INT         NOT NULL,
    z        INT         NOT NULL,
    stage    VARCHAR(64) NOT NULL,
    mined_at BIGINT      NOT NULL,
    PRIMARY KEY (client, world, x, y, z)
);

-- The read path: everything one player has depleted in one node, on entering it.
CREATE INDEX IF NOT EXISTS idx_personal_mine_points_node ON personal_mine_points (client, world, node);

-- The housekeeping path: rows whose respawn elapsed long ago, left behind by players who never came back.
CREATE INDEX IF NOT EXISTS idx_personal_mine_points_mined_at ON personal_mine_points (mined_at);
