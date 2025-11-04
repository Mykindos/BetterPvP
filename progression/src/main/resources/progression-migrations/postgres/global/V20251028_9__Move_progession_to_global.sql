CREATE TABLE IF NOT EXISTS progression_exp
(
    client      BIGINT       NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    season      SMALLINT     NOT NULL,
    profession  VARCHAR(255) NOT NULL,
    experience  BIGINT DEFAULT 0 NOT NULL,
    PRIMARY KEY (client, season, profession)
) PARTITION BY LIST (season);

CREATE TABLE IF NOT EXISTS progression_fishing
(
    id        SERIAL,
    client    BIGINT                  NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    season    SMALLINT                NOT NULL,
    type      VARCHAR(36)             NOT NULL,
    weight    INT                     NOT NULL,
    timestamp BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT NOT NULL,
    PRIMARY KEY (id, season)
) PARTITION BY LIST (season);

CREATE INDEX IF NOT EXISTS idx_progression_fishing_client_season_timestamp_weight
    ON progression_fishing (client, season, timestamp DESC, weight DESC);

CREATE INDEX IF NOT EXISTS idx_progression_fishing_season_timestamp_weight
    ON progression_fishing (season, timestamp DESC, weight DESC);


CREATE TABLE IF NOT EXISTS progression_mining
(
    client       BIGINT      NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    season       SMALLINT    NOT NULL,
    material     VARCHAR(36) NOT NULL,
    amount_mined INTEGER     NOT NULL,
    PRIMARY KEY (client, season, material)
) PARTITION BY LIST (season);

CREATE INDEX IF NOT EXISTS idx_progression_mining_client_season_material
    ON progression_mining (client, season, material);

CREATE INDEX IF NOT EXISTS idx_progression_mining_season_material
    ON progression_mining (season, material);

CREATE TABLE IF NOT EXISTS progression_woodcutting
(
    id        SERIAL,
    client    BIGINT                  NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    season    SMALLINT                NOT NULL,
    material  VARCHAR(50)             NOT NULL,
    location  VARCHAR(285)            NOT NULL,
    amount    INT       DEFAULT 1     NULL,
    timestamp BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT NOT NULL,
    PRIMARY KEY(id, season)
) PARTITION BY LIST (season);


CREATE INDEX IF NOT EXISTS idx_progression_woodcutting_timestamp 
    ON progression_woodcutting (season, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_progression_woodcutting_client_season_timestamp 
    ON progression_woodcutting (client, season, timestamp DESC);

CREATE TABLE IF NOT EXISTS progression_properties
(
    client     BIGINT       NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    season     SMALLINT     NOT NULL,
    profession VARCHAR(255) NOT NULL,
    property   VARCHAR(255) NOT NULL,
    value      VARCHAR(255) NOT NULL,
    PRIMARY KEY (client, season, profession, property)
) PARTITION BY LIST (season);

CREATE TABLE IF NOT EXISTS progression_builds
(
    client     BIGINT       NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    season     SMALLINT     NOT NULL,
    profession VARCHAR(255) NOT NULL,
    skill      VARCHAR(255) NOT NULL,
    level      INTEGER DEFAULT 0 NULL,
    CONSTRAINT progression_builds_pk
        PRIMARY KEY (client, season, profession, skill)
) PARTITION BY LIST (season);

-- Get top fishing by weight
CREATE OR REPLACE FUNCTION get_top_fishing_by_weight(
    season_param INTEGER,
    days DOUBLE PRECISION,
    max_results INTEGER
)
    RETURNS TABLE(client_uuid VARCHAR, total_weight BIGINT) AS $$
BEGIN
    RETURN QUERY
        SELECT clients.uuid, SUM(progression_fishing.weight)
        FROM progression_fishing
        INNER JOIN clients on clients.id = progression_fishing.client
        WHERE season = season_param AND progression_fishing.timestamp > (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT - (days * 24 * 60 * 60 * 1000)::BIGINT
        GROUP BY clients.uuid
        ORDER BY SUM(progression_fishing.weight) DESC
        LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

-- Get top fishing by count
CREATE OR REPLACE FUNCTION get_top_fishing_by_count(
    season_param INTEGER,
    days DOUBLE PRECISION,
    max_results INT
)
    RETURNS TABLE(client_uuid VARCHAR, fish_count BIGINT) AS $$
BEGIN
    RETURN QUERY
        SELECT clients.uuid, COUNT(*)
        FROM progression_fishing
        INNER JOIN clients on clients.id = progression_fishing.client
        WHERE season = season_param AND progression_fishing.timestamp > (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT - (days * 24 * 60 * 60 * 1000)::BIGINT
        GROUP BY clients.uuid
        ORDER BY COUNT(*) DESC
        LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

-- Get client fishing weight
CREATE OR REPLACE FUNCTION get_client_fishing_weight(
    p_client_id BIGINT,
    season_param INTEGER,
    days DOUBLE PRECISION
)
    RETURNS TABLE(total_weight BIGINT) AS $$
BEGIN
    RETURN QUERY
        SELECT SUM(progression_fishing.weight)
        FROM progression_fishing
        WHERE progression_fishing.client = p_client_id
          AND progression_fishing.season = season_param
          AND progression_fishing.timestamp > (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT - (days * 24 * 60 * 60 * 1000)::BIGINT;
END;
$$ LANGUAGE plpgsql;

-- Get client fishing count
CREATE OR REPLACE FUNCTION get_client_fishing_count(
    p_client_id BIGINT,
    season_param INTEGER,
    days DOUBLE PRECISION
)
    RETURNS TABLE(fish_count BIGINT) AS $$
BEGIN
    RETURN QUERY
        SELECT COUNT(*)
        FROM progression_fishing
        WHERE progression_fishing.client = p_client_id
          AND progression_fishing.season = season_param
          AND progression_fishing.timestamp > (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT - (days * 24 * 60 * 60 * 1000)::BIGINT;
END;
$$ LANGUAGE plpgsql;

-- Get biggest fish caught
CREATE OR REPLACE FUNCTION get_biggest_fish_caught(
    season_param INTEGER,
    days DOUBLE PRECISION,
    max_results INT
)
    RETURNS TABLE(id INTEGER, client_uuid VARCHAR, type VARCHAR, weight INT) AS $$
BEGIN
    RETURN QUERY
        SELECT progression_fishing.id, clients.uuid, progression_fishing.type, progression_fishing.weight
        FROM progression_fishing
        INNER JOIN clients on clients.id = progression_fishing.client
        WHERE progression_fishing.season = season_param AND progression_fishing.timestamp > (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT - (days * 24 * 60 * 60 * 1000)::BIGINT
        ORDER BY progression_fishing.weight DESC
        LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

-- Get biggest fish caught by client
CREATE OR REPLACE FUNCTION get_biggest_fish_caught_by_client(
    p_client_id BIGINT,
    season_param INTEGER,
    days DOUBLE PRECISION,
    max_results INT
)
    RETURNS TABLE(id INTEGER, client_uuid VARCHAR, type VARCHAR, weight INT) AS $$
BEGIN
    RETURN QUERY
        SELECT progression_fishing.id, clients.uuid, progression_fishing.type, progression_fishing.weight
        FROM progression_fishing
        INNER JOIN clients on clients.id = progression_fishing.client
        WHERE progression_fishing.timestamp > (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT - (days * 24 * 60 * 60 * 1000)::BIGINT
          AND progression_fishing.client = p_client_id
        AND season_param = progression_fishing.season
        ORDER BY progression_fishing.weight DESC
        LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

-- Get top mining by ore
CREATE OR REPLACE FUNCTION get_top_mining_by_ore(
    season_param INTEGER,
    max_results INT,
    blocks TEXT[]
)
    RETURNS TABLE(client_uuid VARCHAR, total_amount_mined INTEGER) AS $$
BEGIN
    RETURN QUERY
        SELECT clients.uuid, SUM(progression_mining.amount_mined)::INTEGER
        FROM progression_mining
        INNER JOIN clients on clients.id = progression_mining.client
        WHERE progression_mining.material = ANY(blocks)
        AND progression_mining.season = season_param
        GROUP BY clients.uuid
        ORDER BY SUM(progression_mining.amount_mined) DESC
        LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

-- Get client ores mined
CREATE OR REPLACE FUNCTION get_client_ores_mined(
    p_client_id BIGINT,
    season_param INTEGER,
    blocks TEXT[]
)
    RETURNS TABLE(total_amount_mined INTEGER) AS $$
BEGIN
    RETURN QUERY
        SELECT SUM(progression_mining.amount_mined)::INTEGER
        FROM progression_mining
        WHERE progression_mining.client = p_client_id
          AND progression_mining.material = ANY(blocks)
          AND season_param = progression_mining.season;
END;
$$ LANGUAGE plpgsql;