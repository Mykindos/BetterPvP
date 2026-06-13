-- Quest definition ids are human-friendly strings (e.g. "first_blood"), not
-- UUIDs, so the runtime instance's quest_id must be text. Safe on a fresh DB
-- (uuid -> text) and a no-op shape change if already text.
ALTER TABLE quest_instances ALTER COLUMN quest_id TYPE text USING quest_id::text;
