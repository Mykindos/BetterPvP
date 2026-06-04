-- The Fields system has been replaced by the region-scan resource-node framework
-- (clans/world/resource). Ore fields are now defined by tagged Mapper regions and snapshotted
-- in-memory at load, so the per-block persistence table is no longer used.
DROP TABLE IF EXISTS clans_fields_ores;
