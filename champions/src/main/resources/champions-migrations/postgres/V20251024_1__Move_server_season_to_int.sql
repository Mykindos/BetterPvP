CREATE TABLE IF NOT EXISTS champions_killdeath_data
(
    matchup VARCHAR(255) NOT NULL,
    metric  VARCHAR(255) NOT NULL,
    value   INTEGER DEFAULT 0 NULL,
    CONSTRAINT champions_killdeath_data_uk
        UNIQUE (matchup, metric)
);

CREATE TABLE IF NOT EXISTS champions_kills
(
    kill_id      BIGINT PRIMARY KEY REFERENCES kills (id) ON DELETE CASCADE,
    killer_class VARCHAR(20) DEFAULT '',
    victim_class VARCHAR(20) DEFAULT ''
);

-- For faster lookups by killer_class and victim_class in the procedures
CREATE INDEX IF NOT EXISTS idx_champions_kills_killer_class
    ON champions_kills (killer_class);

CREATE INDEX IF NOT EXISTS idx_champions_kills_victim_class
    ON champions_kills (victim_class);

CREATE TABLE IF NOT EXISTS champions_kill_contributions
(
    contribution_id   BIGINT PRIMARY KEY REFERENCES kill_contributions (id) ON DELETE CASCADE,
    contributor_class VARCHAR(20) DEFAULT ''
);

-- For faster lookups by contributor_class
CREATE INDEX IF NOT EXISTS idx_champions_kill_contributions_contributor_class
    ON champions_kill_contributions (contributor_class);

CREATE TABLE IF NOT EXISTS champions_combat_stats
(
    client             BIGINT NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    realm              SMALLINT NOT NULL,
    class              VARCHAR(20)  DEFAULT '',
    rating             INTEGER      NOT NULL,
    killstreak         INTEGER      NOT NULL DEFAULT 0,
    highest_killstreak INTEGER      NOT NULL DEFAULT 0,
    valid              BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Primary lookup pattern used in all procedures
CREATE INDEX IF NOT EXISTS idx_champions_combat_stats_realm_class_valid
    ON champions_combat_stats (realm, class, valid);

-- For ordering by rating
CREATE INDEX IF NOT EXISTS idx_champions_combat_stats_realm_class_rating
    ON champions_combat_stats (realm, class, valid, rating DESC);

-- For ordering by killstreak
CREATE INDEX IF NOT EXISTS idx_champions_combat_stats_realm_class_killstreak
    ON champions_combat_stats (realm, class, valid, killstreak DESC);

-- For ordering by highest_killstreak
CREATE INDEX IF NOT EXISTS idx_champions_combat_stats_realm_class_highest_killstreak
    ON champions_combat_stats (realm, class, valid, highest_killstreak DESC);

-- For the get_champions_data function joining on client
CREATE INDEX IF NOT EXISTS idx_champions_combat_stats_realm_client_valid
    ON champions_combat_stats (realm, client, valid);

CREATE OR REPLACE FUNCTION get_top_rating_by_class(
    realm_var INTEGER,
    top INTEGER,
    class_param VARCHAR(20)
)
    RETURNS TABLE(client VARCHAR(36), rating INTEGER)
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        SELECT c.uuid AS client, r.rating
        FROM champions_combat_stats AS r
        JOIN clients AS c ON r.client = c.id
        WHERE r.realm = realm_var::SMALLINT
          AND r.class = class_param
          AND r.valid = TRUE
        ORDER BY rating DESC
        LIMIT top;
END;
$$;

CREATE OR REPLACE FUNCTION get_top_kills_by_class(
    realm_var INTEGER,
    top INTEGER,
    class_param VARCHAR(20)
)
    RETURNS TABLE(client VARCHAR(36), kills BIGINT)
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        SELECT c.uuid AS client, COUNT(*) AS kills
        FROM kills
                 INNER JOIN champions_kills ON kills.id = champions_kills.kill_id
                 INNER JOIN clients AS c ON kills.killer = c.id
        WHERE kills.realm = realm_var::SMALLINT
          AND champions_kills.killer_class = class_param
          AND kills.valid = TRUE
        GROUP BY c.id
        ORDER BY kills DESC
        LIMIT top;
END;
$$;

CREATE OR REPLACE FUNCTION get_top_deaths_by_class(
    realm_var INTEGER,
    top INTEGER,
    class_param VARCHAR(20)
)
    RETURNS TABLE(client VARCHAR(36), deaths BIGINT)
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        SELECT c.uuid AS client, COUNT(*) AS deaths
        FROM kills
                 INNER JOIN champions_kills ON kills.id = champions_kills.kill_id
                 INNER JOIN clients AS c ON kills.victim = c.id
        WHERE kills.realm = realm_var::SMALLINT
          AND champions_kills.victim_class = class_param
          AND kills.valid = TRUE
        GROUP BY c.id
        ORDER BY deaths DESC
        LIMIT top;
END;
$$;

CREATE OR REPLACE FUNCTION get_top_kdr_by_class(
    realm_var INTEGER,
    top INTEGER,
    class_param VARCHAR(20)
)
    RETURNS TABLE(client VARCHAR(36), kdr NUMERIC)
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        WITH kill_count AS (
            SELECT c.uuid AS client, COUNT(*) AS kills
            FROM kills
                     LEFT JOIN champions_kills ON kills.id = champions_kills.kill_id
                     JOIN clients AS c ON kills.killer = c.id
            WHERE kills.realm = realm_var::SMALLINT
              AND champions_kills.killer_class = class_param
              AND kills.valid = TRUE
            GROUP BY c.id
        ),
             deaths AS (
                 SELECT c.uuid AS client, COUNT(*) AS deaths
                 FROM kills
                          LEFT JOIN champions_kills ON kills.id = champions_kills.kill_id
                          JOIN clients AS c ON kills.victim = c.id
                 WHERE kills.realm = realm_var::SMALLINT
                   AND champions_kills.victim_class = class_param
                   AND kills.valid = TRUE
                 GROUP BY c.id
             )
        SELECT kill_count.client AS client, COALESCE(kill_count.kills::NUMERIC / deaths.deaths, kill_count.kills::NUMERIC) AS kdr
        FROM kill_count
                 LEFT JOIN deaths ON kill_count.client = deaths.client
        ORDER BY kdr DESC
        LIMIT top;
END;
$$;

CREATE OR REPLACE FUNCTION get_top_killstreak_by_class(
    realm_var INTEGER,
    top INTEGER,
    class_param VARCHAR(20)
)
    RETURNS TABLE(client VARCHAR(36), killstreak INTEGER)
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        SELECT c.uuid AS client, k.killstreak
        FROM champions_combat_stats AS k
        JOIN clients AS c ON k.client = c.id
        WHERE k.realm = realm_var::SMALLINT
          AND k.class = class_param
          AND k.valid = TRUE
        ORDER BY killstreak DESC
        LIMIT top;
END;
$$;

CREATE OR REPLACE FUNCTION get_top_highest_killstreak_by_class(
    realm_var INTEGER,
    top INTEGER,
    class_param VARCHAR(20)
)
    RETURNS TABLE(client VARCHAR(36), highest_killstreak INTEGER)
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        SELECT c.uuid AS client, k.highest_killstreak
        FROM champions_combat_stats AS k
        JOIN clients AS c ON k.client = c.id
        WHERE k.realm = realm_var::SMALLINT
          AND k.class = class_param
          AND k.valid = TRUE
        ORDER BY highest_killstreak DESC
        LIMIT top;
END;
$$;

CREATE OR REPLACE FUNCTION get_champions_data(
    client_uuid VARCHAR(36),
    realm_var INTEGER
)
    RETURNS TABLE(
                     class VARCHAR(20),
                     kills BIGINT,
                     deaths BIGINT,
                     assists BIGINT,
                     rating INTEGER,
                     killstreak INTEGER,
                     highest_killstreak INTEGER
                 )
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        SELECT cs.class,
               COALESCE(SUM(kills_count), 0)::BIGINT   AS kills,
               COALESCE(SUM(deaths_count), 0)::BIGINT  AS deaths,
               COALESCE(SUM(assists_count), 0)::BIGINT AS assists,
               MAX(cs.rating)                          AS rating,
               MAX(cs.killstreak)                      AS killstreak,
               MAX(cs.highest_killstreak)              AS highest_killstreak
        FROM champions_combat_stats AS cs
                 INNER JOIN clients AS c ON cs.client = c.id
                 LEFT JOIN LATERAL (SELECT ck.killer_class, COUNT(kill_id) AS kills_count
                                    FROM kills
                                             LEFT JOIN champions_kills AS ck ON ck.kill_id = kills.id
                                    WHERE kills.realm = realm_var::SMALLINT
                                      AND kills.killer = c.id
                                    GROUP BY ck.killer_class) AS k ON cs.class = k.killer_class
                 LEFT JOIN LATERAL (SELECT ck.victim_class, COUNT(kill_id) AS deaths_count
                                    FROM kills
                                             LEFT JOIN champions_kills AS ck ON ck.kill_id = kills.id
                                    WHERE kills.realm = realm_var::SMALLINT
                                      AND kills.victim = c.id
                                    GROUP BY ck.victim_class) AS d ON cs.class = d.victim_class
                 LEFT JOIN LATERAL (SELECT ckc.contributor_class, COUNT(kill_id) AS assists_count
                                    FROM kill_contributions
                                             LEFT JOIN champions_kill_contributions AS ckc ON ckc.contribution_id = kill_contributions.id
                                    WHERE kill_contributions.contributor = c.id
                                    GROUP BY ckc.contributor_class) AS ac ON cs.class = ac.contributor_class
        WHERE cs.realm = realm_var::SMALLINT
          AND c.uuid = client_uuid
          AND cs.valid = TRUE
        GROUP BY cs.class;
END;
$$;