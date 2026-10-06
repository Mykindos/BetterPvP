-- Every release the pack server publishes to a channel, newest last. The pack server inserts a row and sends
-- NOTIFY resource_pack_published with the channel; servers read the newest row of their channel.
CREATE TABLE IF NOT EXISTS resource_pack_release
(
    id          BIGSERIAL PRIMARY KEY,
    channel     VARCHAR(32) NOT NULL,
    manifest    JSONB       NOT NULL,
    released_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS resource_pack_release_channel ON resource_pack_release (channel, id DESC);
