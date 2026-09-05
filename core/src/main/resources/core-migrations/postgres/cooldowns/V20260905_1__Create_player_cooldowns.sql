-- Cooldowns that have to outlive the session and the server that set them.
--
-- The in-memory CooldownManager is right for abilities: it is fast, it is per-tick, and a cooldown that resets when you
-- reconnect costs nothing there. It is wrong for anything a player would gain by relogging to clear -- and wrong again
-- if spawn is ever sharded, since a limit held in one server's memory is no limit at all to a player who hops to
-- another.
--
-- One row per (client, key), holding the moment it expires. Absent or past means not on cooldown, so expiry needs no
-- sweep to be correct; the prune only stops the table growing.
CREATE TABLE IF NOT EXISTS player_cooldowns
(
    client       BIGINT      NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    cooldown_key VARCHAR(64) NOT NULL,
    expires_at   BIGINT      NOT NULL,
    PRIMARY KEY (client, cooldown_key)
);

CREATE INDEX IF NOT EXISTS idx_player_cooldowns_expires_at ON player_cooldowns (expires_at);
