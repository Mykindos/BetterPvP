-- ============================================================
-- Count UUID items last seen within an X/Z bounding box,
-- grouped by item name.
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

-- ── 2. Most-recent ITEM_ log per UUID that has a Location ──
latest_item_logs AS (
    SELECT DISTINCT ON (lc_item.value)
        lc_item.value AS item_uuid,
        lc_name.value AS item_name,
        lc_loc.value  AS location
    FROM params, logs l
    JOIN logs_context lc_item ON lc_item.log_id = l.id AND lc_item.context = 'Item'
    JOIN logs_context lc_loc  ON lc_loc.log_id  = l.id AND lc_loc.context  = 'Location'
    LEFT JOIN logs_context lc_name ON lc_name.log_id = l.id AND lc_name.context = 'ItemName'
    WHERE l.realm  = params.realm_id
      AND l.action LIKE 'ITEM_%'
    ORDER BY lc_item.value, l.log_time DESC
),

-- ── 3. Parse X and Z from "(world, X, Y, Z)" ───────────────
parsed AS (
    SELECT
        COALESCE(item_name, 'unknown') AS item_name,
        CAST(
            TRIM(SPLIT_PART(REPLACE(REPLACE(location, '(', ''), ')', ''), ', ', 2))
        AS INTEGER) AS loc_x,
        CAST(
            TRIM(SPLIT_PART(REPLACE(REPLACE(location, '(', ''), ')', ''), ', ', 4))
        AS INTEGER) AS loc_z
    FROM latest_item_logs
)

-- ── 4. Count per item name within bounds ───────────────────
SELECT
    item_name   AS "Item Name",
    COUNT(*)    AS "Count"
FROM parsed, params
WHERE loc_x BETWEEN LEAST(params.min_x, params.max_x)
                AND GREATEST(params.min_x, params.max_x)
  AND loc_z BETWEEN LEAST(params.min_z, params.max_z)
                AND GREATEST(params.min_z, params.max_z)
GROUP BY item_name
ORDER BY "Count" DESC, item_name;

