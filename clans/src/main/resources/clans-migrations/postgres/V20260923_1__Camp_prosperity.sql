-- Each camp's Prosperity, so the leaderboard can rank camps whose records are not loaded on this server.
-- Written by whichever server holds the camp's world, every few minutes.
CREATE TABLE IF NOT EXISTS camp_prosperity
(
    clan       BIGINT PRIMARY KEY REFERENCES clans (id) ON DELETE CASCADE,
    prosperity INT    NOT NULL,
    updated_at BIGINT NOT NULL
);

-- The leaderboard's read path.
CREATE INDEX IF NOT EXISTS idx_camp_prosperity_prosperity ON camp_prosperity (prosperity DESC);
