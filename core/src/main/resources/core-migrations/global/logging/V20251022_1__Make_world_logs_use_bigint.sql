-- Drop tables if they exist (metadata table first due to foreign key constraint)
DROP TABLE IF EXISTS world_logs_metadata;
DROP TABLE IF EXISTS world_logs;

-- Recreate world_logs table with bigint primary key and bigint Time column
CREATE TABLE IF NOT EXISTS world_logs
(
    id        BIGINT            NOT NULL,
    Server    TINYINT UNSIGNED  NOT NULL,
    Season    TINYINT UNSIGNED  NOT NULL,
    World     VARCHAR(255)      NOT NULL,
    BlockX    INT               NOT NULL,
    BlockY    INT               NOT NULL,
    BlockZ    INT               NOT NULL,
    Action    VARCHAR(255)      NOT NULL,
    Material  VARCHAR(255)      NOT NULL,
    BlockData TEXT              NULL,
    ItemStack MEDIUMBLOB        NULL,
    Time      BIGINT            NOT NULL,
    PRIMARY KEY (id, Server, Season)
)
    PARTITION BY LIST COLUMNS(Server, Season) (
        PARTITION p_default VALUES IN ((0, 0))
        );

-- Recreate indexes for world_logs
CREATE INDEX world_logs_location_index
    ON world_logs (Server, Season, World, BlockX, BlockY, BlockZ, Time);

CREATE INDEX world_logs_time_index
    ON world_logs (Server, Season, Time);

CREATE INDEX world_logs_world_action_index
    ON world_logs (Server, Season, World, Action, Time);

-- Recreate world_logs_metadata table with bigint LogId
CREATE TABLE IF NOT EXISTS world_logs_metadata
(
    LogId     BIGINT           NOT NULL,
    Server    TINYINT UNSIGNED NOT NULL,
    Season    TINYINT UNSIGNED NOT NULL,
    MetaKey   VARCHAR(255)     NOT NULL,
    MetaValue VARCHAR(255)     NOT NULL,
    PRIMARY KEY (LogId, Server, Season, MetaKey)
)
    PARTITION BY LIST COLUMNS(Server, Season) (
        PARTITION p_default VALUES IN ((0, 0))
        );

-- Recreate indexes for world_logs_metadata
CREATE INDEX world_logs_metadata_key_value_index
    ON world_logs_metadata (Server, Season, MetaKey, MetaValue);

CREATE INDEX world_logs_metadata_value_index
    ON world_logs_metadata (Server, Season, MetaValue);