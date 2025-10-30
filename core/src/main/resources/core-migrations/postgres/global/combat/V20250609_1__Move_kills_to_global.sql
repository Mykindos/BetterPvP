CREATE TABLE IF NOT EXISTS kills
(
    id            SERIAL PRIMARY KEY,
    realm         SMALLINT       NOT NULL,
    killer        BIGINT        NOT NULL,
    victim        BIGINT        NOT NULL,
    contribution  REAL           NOT NULL,
    damage        REAL           NOT NULL,
    rating_delta  INTEGER        NOT NULL,
    time          BIGINT         NOT NULL,
    valid         BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_kills_killer ON kills (realm, killer);
CREATE INDEX IF NOT EXISTS idx_kills_victim ON kills (realm, victim);
CREATE INDEX IF NOT EXISTS idx_kills_killer_victim ON kills (realm, killer, victim);

CREATE TABLE IF NOT EXISTS kill_contributions
(
    id           SERIAL NOT NULL PRIMARY KEY,
    kill_id      INTEGER NOT NULL REFERENCES kills (id),
    contributor  BIGINT  NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    contribution REAL NOT NULL,
    damage       REAL NOT NULL,
    UNIQUE (kill_id, contributor)
);

CREATE INDEX IF NOT EXISTS idx_kill_contributions_contributor ON kill_contributions (contributor);

CREATE TABLE IF NOT EXISTS combat_stats
(
    client             BIGINT NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    realm              SMALLINT NOT NULL,
    rating             INTEGER NOT NULL,
    killstreak         INTEGER NOT NULL DEFAULT 0,
    highest_killstreak INTEGER NOT NULL DEFAULT 0,
    valid              BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (client, realm)
);


DROP FUNCTION IF EXISTS get_combat_data(VARCHAR, SMALLINT);

CREATE OR REPLACE FUNCTION get_combat_data(client_uuid VARCHAR(36), realm_param INTEGER)
    RETURNS TABLE(kills INTEGER, deaths INTEGER, assists INTEGER, rating INTEGER, killstreak INTEGER, highest_killstreak INTEGER)
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        SELECT COALESCE(SUM(k.kills_count), 0)::INTEGER    AS kills,
               COALESCE(SUM(d.deaths_count), 0)::INTEGER   AS deaths,
               COALESCE(SUM(ac.assists_count), 0)::INTEGER AS assists,
               MAX(cs.rating)                              AS rating,
               MAX(cs.killstreak)                          AS killstreak,
               MAX(cs.highest_killstreak)                  AS highest_killstreak
        FROM combat_stats AS cs
                 LEFT JOIN (SELECT killer, COUNT(*) AS kills_count
                            FROM kills
                            GROUP BY killer) AS k ON cs.client = k.killer
                 LEFT JOIN (SELECT victim, COUNT(*) AS deaths_count
                            FROM kills
                            GROUP BY victim) AS d ON cs.client = d.victim
                 LEFT JOIN (SELECT contributor, COUNT(*) AS assists_count
                            FROM kill_contributions
                            GROUP BY contributor) AS ac ON cs.client = ac.contributor
                 INNER JOIN clients AS c ON cs.client = c.id
        WHERE c.uuid = client_uuid
          AND cs.realm = realm_param
          AND cs.valid = TRUE;
END;
$$;

CREATE OR REPLACE FUNCTION get_top_rating(realm_param INTEGER, top INTEGER)
    RETURNS TABLE(client BIGINT, rating INTEGER)
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        SELECT cs.client, cs.rating
        FROM combat_stats cs
        WHERE cs.realm = realm_param
          AND cs.valid = TRUE
        ORDER BY cs.rating DESC
        LIMIT top;
END;
$$;

CREATE OR REPLACE FUNCTION get_top_kills(realm_param INTEGER, top INTEGER)
    RETURNS TABLE(gamer BIGINT, kills BIGINT)
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        SELECT k.killer AS gamer, COUNT(*) AS kills
        FROM kills k
        WHERE k.realm = realm_param
          AND k.valid = TRUE
        GROUP BY gamer
        ORDER BY kills DESC
        LIMIT top;
END;
$$;

CREATE OR REPLACE FUNCTION get_top_deaths(realm_param INTEGER, top INTEGER)
    RETURNS TABLE(gamer BIGINT, deaths BIGINT)
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        SELECT k.victim AS gamer, COUNT(*) AS deaths
        FROM kills k
        WHERE k.realm = realm_param
          AND k.valid = TRUE
        GROUP BY gamer
        ORDER BY deaths DESC
        LIMIT top;
END;
$$;

CREATE OR REPLACE FUNCTION get_top_kdr(realm_param INTEGER, top INTEGER)
    RETURNS TABLE(gamer BIGINT, kdr NUMERIC)
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        WITH kill_count AS (SELECT killer AS gamer, COUNT(*) AS kills
                            FROM kills
                            WHERE realm = realm_param
                              AND valid = TRUE
                            GROUP BY killer),
             deaths AS (SELECT victim AS gamer, COUNT(*) AS deaths
                        FROM kills
                        WHERE realm = realm_param
                          AND valid = TRUE
                        GROUP BY victim)

        SELECT kill_count.gamer AS gamer, COALESCE(kill_count.kills::NUMERIC / deaths.deaths, kill_count.kills::NUMERIC) AS kdr
        FROM kill_count
                 LEFT JOIN deaths ON kill_count.gamer = deaths.gamer
        ORDER BY kdr DESC
        LIMIT top;
END;
$$;

CREATE OR REPLACE FUNCTION get_top_killstreak(realm_param INTEGER, top INTEGER)
    RETURNS TABLE(client BIGINT, killstreak INTEGER)
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        SELECT cs.client, cs.killstreak
        FROM combat_stats cs
        WHERE cs.realm = realm_param
          AND cs.valid = TRUE
        ORDER BY cs.killstreak DESC
        LIMIT top;
END;
$$;

CREATE OR REPLACE FUNCTION get_top_highest_killstreak(realm_param INTEGER, top INTEGER)
    RETURNS TABLE(client BIGINT, highest_killstreak INTEGER)
    LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
        SELECT cs.client, cs.highest_killstreak
        FROM combat_stats cs
        WHERE cs.realm = realm_param
          AND cs.valid = TRUE
        ORDER BY cs.highest_killstreak DESC
        LIMIT top;
END;
$$;