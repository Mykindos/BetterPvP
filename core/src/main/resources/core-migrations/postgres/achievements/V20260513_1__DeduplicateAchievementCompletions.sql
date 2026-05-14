-- ============================================================
-- Migration: Deduplicate achievement completions
--
-- A bug caused multiple completion rows to be saved for the
-- same achievement. This migration keeps only the earliest
-- (lowest Timestamp) completion per group:
--   - ALL achievements : one per (Client, Namespace, Keyname)
--   - Season achievements : one per (Client, Namespace, Keyname, Season)
--   - Realm  achievements : one per (Client, Namespace, Keyname, Realm)
--
-- Child rows (season / realm) are removed before the parent
-- to satisfy the foreign-key constraints.
-- ============================================================

-- 1. Identify the seasonal completion IDs to KEEP
--    (earliest Timestamp per Client + achievement key + Season)
CREATE TEMP TABLE _season_keepers AS
SELECT DISTINCT ON (ac.Client, ac.Namespace, ac.Keyname, acs.Season)
    acs.Id
FROM achievement_completions_season acs
JOIN achievement_completions ac ON ac.Id = acs.Id
ORDER BY ac.Client, ac.Namespace, ac.Keyname, acs.Season, ac.Timestamp ASC;

-- 2. Identify the realm completion IDs to KEEP
--    (earliest Timestamp per Client + achievement key + Realm)
CREATE TEMP TABLE _realm_keepers AS
SELECT DISTINCT ON (ac.Client, ac.Namespace, ac.Keyname, acr.Realm)
    acr.Id
FROM achievement_completions_realm acr
JOIN achievement_completions ac ON ac.Id = acr.Id
ORDER BY ac.Client, ac.Namespace, ac.Keyname, acr.Realm, ac.Timestamp ASC;

-- 3. Identify ALL-type completion IDs to KEEP
--    (earliest Timestamp per Client + achievement key, no season/realm)
CREATE TEMP TABLE _all_keepers AS
SELECT DISTINCT ON (ac.Client, ac.Namespace, ac.Keyname)
    ac.Id
FROM achievement_completions ac
WHERE NOT EXISTS (SELECT 1 FROM achievement_completions_season acs WHERE acs.Id = ac.Id)
  AND NOT EXISTS (SELECT 1 FROM achievement_completions_realm  acr WHERE acr.Id = ac.Id)
ORDER BY ac.Client, ac.Namespace, ac.Keyname, ac.Timestamp ASC;

-- 4. Remove duplicate seasonal child rows
DELETE FROM achievement_completions_season
WHERE Id NOT IN (SELECT Id FROM _season_keepers);

-- 5. Remove duplicate realm child rows
DELETE FROM achievement_completions_realm
WHERE Id NOT IN (SELECT Id FROM _realm_keepers);

-- 6. Remove duplicate base rows
--    Any row not retained as an ALL-keeper, season-keeper, or realm-keeper is a
--    duplicate and can be safely deleted (its child rows were already removed above).
DELETE FROM achievement_completions
WHERE Id NOT IN (SELECT Id FROM _all_keepers)
  AND Id NOT IN (SELECT Id FROM _season_keepers)
  AND Id NOT IN (SELECT Id FROM _realm_keepers);

-- Cleanup
DROP TABLE _season_keepers;
DROP TABLE _realm_keepers;
DROP TABLE _all_keepers;

