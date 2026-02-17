-- Tracks per-member XP contributions to their clan's total experience.
-- Used for leaderboards and recognizing top contributors.
CREATE TABLE IF NOT EXISTS clan_xp_contributions
(
    clan         BIGINT           NOT NULL REFERENCES clans (id) ON DELETE CASCADE,
    member       VARCHAR(36)      NOT NULL,
    contribution DOUBLE PRECISION NOT NULL DEFAULT 0,
    PRIMARY KEY (clan, member)
);

CREATE INDEX IF NOT EXISTS idx_clan_xp_contributions_clan
    ON clan_xp_contributions (clan);
