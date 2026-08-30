-- ============================================================
-- Retrieve all UUID items last seen within an X/Z bounding box
-- on a specific realm.
--
-- HOW TO USE:
--   Edit the four values in the "params" CTE below:
--     min_x, max_x  ->  X coordinate bounds
--     min_z, max_z  ->  Z coordinate bounds
--   The realm is set to 6 (change if needed).
-- ============================================================

WITH

-- ── 1. Edit your search parameters here ────────────────────
params AS (
    SELECT
        6    AS realm_id,
        -500 AS min_x,
        -500 AS min_z,
        500  AS max_x,
        500  AS max_z
),

-- ── 2. Find the most-recent ITEM_ log per UUID that has a
--       Location context entry (DISTINCT ON keeps only the
--       row with the highest log_time per item UUID).       ──
latest_item_logs AS (
    SELECT DISTINCT ON (lc_item.value)
        lc_item.value AS item_uuid,
        lc_name.value AS item_name,
        lc_loc.value  AS location,
        l.log_time
    FROM params, logs l
    JOIN logs_context lc_item ON lc_item.log_id = l.id AND lc_item.context = 'Item'
    JOIN logs_context lc_loc  ON lc_loc.log_id  = l.id AND lc_loc.context  = 'Location'
    LEFT JOIN logs_context lc_name ON lc_name.log_id = l.id AND lc_name.context = 'ItemName'
    WHERE l.realm  = params.realm_id
      AND l.action LIKE 'ITEM_%'
    ORDER BY lc_item.value, l.log_time DESC
),

-- ── 3. Parse X and Z out of the stored location string.
--       Format written by locationToString(loc, true, true):
--           "(world, X, Y, Z)"  e.g. "(world, 142, 64, -88)"  ──
parsed AS (
    SELECT
        item_uuid,
        COALESCE(item_name, 'unknown') AS item_name,
        location,
        log_time,
        CAST(
            TRIM(SPLIT_PART(REPLACE(REPLACE(location, '(', ''), ')', ''), ', ', 2))
        AS INTEGER) AS loc_x,
        CAST(
            TRIM(SPLIT_PART(REPLACE(REPLACE(location, '(', ''), ')', ''), ', ', 4))
        AS INTEGER) AS loc_z
    FROM latest_item_logs
)

-- ── 4. Final result: only items whose last location is inside
--       the requested bounding box.                          ──
SELECT
    item_uuid                                  AS "UUID",
    item_name                                  AS "Item Name",
    location                                   AS "Last Location",
    to_timestamp(log_time / 1000.0)            AS "Last Seen (UTC)"
FROM parsed, params
WHERE loc_x BETWEEN LEAST(params.min_x, params.max_x)
                AND GREATEST(params.min_x, params.max_x)
  AND loc_z BETWEEN LEAST(params.min_z, params.max_z)
                AND GREATEST(params.min_z, params.max_z)
ORDER BY item_name, item_uuid;

