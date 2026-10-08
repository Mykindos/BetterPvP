-- Prosperity rows no longer record when they were last written.
ALTER TABLE camp_prosperity DROP COLUMN IF EXISTS updated_at;
