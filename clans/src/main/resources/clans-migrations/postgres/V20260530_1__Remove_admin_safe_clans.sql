-- Server-owned areas (spawn, shops, Fields, ...) used to be modelled as "admin"/"safe" clans. They are now Mapper
-- region zones loaded by ClanRegionZoneLoader, so the legacy clans are no longer needed.
--
-- Delete every admin/safe clan. The foreign keys on the related tables (clan_territory, clan_members,
-- clan_metadata, clan_properties, clan_alliances, clan_enemies, ...) cascade on this delete, so the chunks those
-- clans claimed become unclaimed and are protected by the overlapping region zones instead.
DELETE FROM clans WHERE admin <> 0 OR safe <> 0;
