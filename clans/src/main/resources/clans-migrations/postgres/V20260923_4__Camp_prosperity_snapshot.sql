-- A daily snapshot of each camp's Prosperity, so the rival board can show how it changed.
ALTER TABLE camp_prosperity ADD COLUMN IF NOT EXISTS snapshot INT NOT NULL DEFAULT 0;
ALTER TABLE camp_prosperity ADD COLUMN IF NOT EXISTS snapshot_at BIGINT NOT NULL DEFAULT 0;
UPDATE camp_prosperity SET snapshot = prosperity;
