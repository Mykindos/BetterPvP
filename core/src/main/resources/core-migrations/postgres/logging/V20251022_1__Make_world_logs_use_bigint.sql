CREATE TABLE IF NOT EXISTS world_logs
(
    id         BIGINT       NOT NULL,
    realm      SMALLINT     NOT NULL,
    world      VARCHAR(255) NOT NULL,
    block_x    INTEGER      NOT NULL,
    block_y    INTEGER      NOT NULL,
    block_z    INTEGER      NOT NULL,
    action     VARCHAR(255) NOT NULL,
    material   VARCHAR(255) NOT NULL,
    block_data TEXT         NULL,
    item_stack BYTEA        NULL,
    time       BIGINT       NOT NULL,
    PRIMARY KEY (id, realm)
) PARTITION BY LIST (realm);

-- Create default partition
CREATE TABLE world_logs_default PARTITION OF world_logs
    FOR VALUES IN (0);

-- Recreate indexes for world_logs
CREATE INDEX world_logs_location_index
    ON world_logs (realm, world, block_x, block_y, block_z, time);

CREATE INDEX world_logs_time_index
    ON world_logs (realm, time);

CREATE INDEX world_logs_world_action_index
    ON world_logs (realm, world, action, time);

-- Recreate world_logs_metadata table with bigint log_id
CREATE TABLE IF NOT EXISTS world_logs_metadata
(
    log_id     BIGINT       NOT NULL,
    realm      SMALLINT     NOT NULL,
    meta_key   VARCHAR(255) NOT NULL,
    meta_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (log_id, realm, meta_key)
) PARTITION BY LIST (realm);

-- Create default partition for metadata
CREATE TABLE world_logs_metadata_default PARTITION OF world_logs_metadata
    FOR VALUES IN (0);

-- Recreate indexes for world_logs_metadata
CREATE INDEX world_logs_metadata_key_value_index
    ON world_logs_metadata (realm, meta_key, meta_value);

CREATE INDEX world_logs_metadata_value_index
    ON world_logs_metadata (realm, meta_value);

CREATE OR REPLACE FUNCTION get_world_logs_for_block(
    realm_param INTEGER,
    world_param VARCHAR(255),
    block_x_param INTEGER,
    block_y_param INTEGER,
    block_z_param INTEGER,
    page_offset INTEGER,
    page_limit INTEGER
)
    RETURNS TABLE (
                      id BIGINT,
                      world VARCHAR(255),
                      block_x INTEGER,
                      block_y INTEGER,
                      block_z INTEGER,
                      action VARCHAR(255),
                      material VARCHAR(255),
                      block_data TEXT,
                      itemstack BYTEA,
                      time_val BIGINT,
                      metadata JSON,
                      total BIGINT
                  )
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        SELECT
            wl.id,
            wl.world,
            wl.block_x,
            wl.block_y,
            wl.block_z,
            wl.action,
            wl.material,
            wl.block_data,
            wl.item_stack,
            wl.time,
            (
                SELECT json_agg(
                               json_build_object(
                                       'Key', wlm.meta_key,
                                       'Value', wlm.meta_value
                               )
                       )
                FROM world_logs_metadata wlm
                WHERE wlm.log_id = wl.id
                  AND wlm.realm = wl.realm
            ) AS metadata,
            COUNT(*) OVER() AS total
        FROM world_logs wl
        WHERE wl.realm = realm_param
          AND wl.world = world_param
          AND wl.block_x = block_x_param
          AND wl.block_y = block_y_param
          AND wl.block_z = block_z_param
        ORDER BY wl.time DESC
        LIMIT page_limit
            OFFSET page_offset;
END;
$$;