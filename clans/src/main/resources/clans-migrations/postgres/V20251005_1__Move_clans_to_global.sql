CREATE TABLE IF NOT EXISTS clans
(
    id        BIGINT      PRIMARY KEY,
    realm     SMALLINT    NOT NULL,
    name      VARCHAR(32) NOT NULL,
    home      VARCHAR(64) NULL,
    admin     SMALLINT    NULL DEFAULT 0,
    safe      SMALLINT    NULL DEFAULT 0,
    UNIQUE (realm, name)
);

CREATE TABLE IF NOT EXISTS clan_metadata
(
    clan      BIGINT PRIMARY KEY REFERENCES clans (id) ON DELETE CASCADE,
    banner    TEXT   NULL,
    mailbox   TEXT   NULL,
    vault     TEXT   NULL
);

CREATE TABLE IF NOT EXISTS clan_territory
(
    id        SERIAL PRIMARY KEY,
    clan      BIGINT NOT NULL REFERENCES clans (id) ON DELETE CASCADE,
    chunk     VARCHAR(64) NOT NULL,
    UNIQUE (clan, chunk)
);

CREATE TABLE IF NOT EXISTS clan_members
(
    id        SERIAL PRIMARY KEY,
    clan      BIGINT NOT NULL REFERENCES clans (id) ON DELETE CASCADE,
    member    VARCHAR(36) NOT NULL,
    rank      VARCHAR(64) NOT NULL DEFAULT 'RECRUIT',
    UNIQUE (clan, member)
);

CREATE TABLE IF NOT EXISTS clan_alliances
(
    id        SERIAL PRIMARY KEY,
    clan      BIGINT NOT NULL REFERENCES clans (id) ON DELETE CASCADE,
    ally_clan BIGINT NOT NULL REFERENCES clans (id) ON DELETE CASCADE,
    trusted   SMALLINT    DEFAULT 0,
    UNIQUE (clan, ally_clan)
);

CREATE TABLE IF NOT EXISTS clan_enemies
(
    id          SERIAL PRIMARY KEY,
    clan        BIGINT NOT NULL REFERENCES clans (id) ON DELETE CASCADE,
    enemy_clan  BIGINT NOT NULL REFERENCES clans (id) ON DELETE CASCADE,
    dominance   SMALLINT    DEFAULT 0,
    UNIQUE (clan, enemy_clan)
);


CREATE TABLE IF NOT EXISTS clans_dominance_scale
(
    clan_size  INT    NOT NULL PRIMARY KEY,
    dominance  DOUBLE PRECISION NOT NULL
);

INSERT INTO clans_dominance_scale VALUES (0, 3.5);
INSERT INTO clans_dominance_scale VALUES (1, 3.5);
INSERT INTO clans_dominance_scale VALUES (2, 3.5);
INSERT INTO clans_dominance_scale VALUES (3, 4);
INSERT INTO clans_dominance_scale VALUES (4, 4);
INSERT INTO clans_dominance_scale VALUES (5, 4.5);
INSERT INTO clans_dominance_scale VALUES (6, 4.5);
INSERT INTO clans_dominance_scale VALUES (7, 5);
INSERT INTO clans_dominance_scale VALUES (8, 5);

CREATE TABLE IF NOT EXISTS clan_insurance
(
    clan            BIGINT NOT NULL REFERENCES clans (id) ON DELETE CASCADE,
    insurance_type  VARCHAR(255)  NOT NULL,
    material        VARCHAR(255)  NOT NULL,
    data            TEXT          NULL,
    time            BIGINT        NOT NULL,
    x               INT           NOT NULL,
    y               INT           NOT NULL,
    z               INT           NOT NULL
);

CREATE INDEX IF NOT EXISTS clan_insurance_clan_time_index
    ON clan_insurance (clan, time desc);

CREATE INDEX IF NOT EXISTS clan_insurance_Time_index
    ON clan_insurance (time desc);

CREATE TABLE IF NOT EXISTS clan_properties
(
    clan     BIGINT NOT NULL REFERENCES clans (id) ON DELETE CASCADE,
    property VARCHAR(255) NOT NULL,
    value    TEXT         NULL,
    PRIMARY KEY (clan, property)
);

CREATE TABLE IF NOT EXISTS clans_fields_ores
(
    realm  SMALLINT    NOT NULL,
    world  VARCHAR(64) NOT NULL,
    x      INT         NOT NULL,
    y      INT         NOT NULL,
    z      INT         NOT NULL,
    type   VARCHAR(64) NOT NULL,
    data   VARCHAR(255) NULL,
    PRIMARY KEY (realm, world, x, y, z)
);

CREATE TABLE IF NOT EXISTS clans_kills
(
    kill_id     BIGINT PRIMARY KEY REFERENCES kills (id) ON DELETE CASCADE,
    killer_clan BIGINT NOT NULL REFERENCES clans (id) ON DELETE CASCADE,
    victim_clan BIGINT NOT NULL REFERENCES clans (id) ON DELETE CASCADE,
    dominance   DOUBLE PRECISION DEFAULT 0
);

CREATE INDEX IF NOT EXISTS clans_kills_killer_clan_index
    ON clans_kills (killer_clan);

CREATE INDEX IF NOT EXISTS clans_kills_victim_clan_index
    ON clans_kills (victim_clan);

DROP PROCEDURE IF EXISTS get_clan_kill_logs(INTEGER);
CREATE OR REPLACE FUNCTION get_clan_kill_logs(p_clan_id BIGINT)
    RETURNS TABLE (
                      killer BIGINT,
                      killer_name VARCHAR,
                      killer_clan BIGINT,
                      victim BIGINT,
                      victim_name VARCHAR,
                      victim_clan BIGINT,
                      dominance DOUBLE PRECISION,
                      time_val BIGINT
                  ) AS $$
BEGIN
    RETURN QUERY
        SELECT kills.killer,
               (SELECT name FROM clients WHERE uuid = kills.killer) AS killer_name,
               clans_kills.killer_clan,
               kills.victim,
               (SELECT name FROM clients WHERE uuid = kills.victim) AS victim_name,
               clans_kills.victim_clan,
               clans_kills.dominance,
               kills.time
        FROM clans_kills
                 INNER JOIN kills ON kills.id = clans_kills.kill_id
        WHERE clans_kills.killer_clan = p_clan_id
           OR clans_kills.victim_clan = p_clan_id
        ORDER BY kills.time DESC
        LIMIT 1000;
END;
$$ LANGUAGE plpgsql;