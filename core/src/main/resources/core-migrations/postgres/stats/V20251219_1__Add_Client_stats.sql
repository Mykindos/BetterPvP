CREATE TABLE IF NOT EXISTS seasons (
    id  SMALLINT     PRIMARY KEY,
    Start   DATE         NOT NULL  DEFAULT CURRENT_DATE,
    Name    varchar(255) NOT NULL
);

INSERT INTO seasons (id, Start, Name) VALUES (0, '2024-10-30', 'Legacy') ON CONFLICT DO NOTHING;

ALTER TABLE realms
ADD CONSTRAINT FK_Realm_Season
FOREIGN KEY (season) REFERENCES seasons(id);

CREATE TABLE IF NOT EXISTS client_stats (
    Client      BIGINT          REFERENCES clients(id)    NOT NULL,
    Realm       integer         REFERENCES realms(id)   NOT NULL,
    StatType    VARCHAR(127)    NOT NULL,
    StatData    JSONB           NOT NULL DEFAULT '{}',
    Stat        BIGINT          NOT NULL,
    CONSTRAINT PK_Client PRIMARY KEY (Client, Realm, StatType, StatData)
);

CREATE INDEX IF NOT EXISTS IDX_Stat_Client
ON client_stats (Client);

CREATE TABLE IF NOT EXISTS game_data (
    id          BIGINT          PRIMARY KEY,
    Game        varchar(50)     NOT NULL,
    Map         varchar(50)     NOT NULL
);

CREATE TABLE IF NOT EXISTS game_teams (
    id          BIGINT      REFERENCES game_data(id)    NOT NULL,
    Client      BIGINT      REFERENCES clients(id)      NOT NULL,
    Team        varchar(20)                             NOT NULL,
    CONSTRAINT PK_Game_teams PRIMARY KEY (id, client)
);

CREATE INDEX IF NOT EXISTS IDX_Teams
ON game_teams(id, Client);

CREATE OR REPLACE FUNCTION get_client_stats(
    client_param BIGINT
)
    RETURNS TABLE(
        Realm Integer,
        StatType VARCHAR(127),
        Data JSONB,
        Stat BIGINT
            ) LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        cs.Realm,
        cs.StatType,
        cs.StatData
            || COALESCE(
                jsonb_build_object(
                    'gameName', gd.Game,
                    'mapName',  gd.Map,
                    'teamName', gt.Team
                ),
                '{}'::jsonb
            ) AS StatData,
        cs.Stat
    FROM client_stats cs
    LEFT JOIN game_data gd
        ON gd.id = (cs.StatData ->> 'gameId')::bigint
    LEFT JOIN game_teams gt
        ON gt.id = gd.id
        AND gt.client = cs.client;
END;
$$;
