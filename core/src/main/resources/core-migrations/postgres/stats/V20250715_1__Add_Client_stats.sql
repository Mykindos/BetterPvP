CREATE TABLE IF NOT EXISTS period_meta (
    Period  VARCHAR(127)                PRIMARY KEY,
    Start   DATE     NOT NULL  DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS client_stats (
    Client      BIGINT          REFERENCES clients(id)    NOT NULL,
    Period      VARCHAR(127)    NOT NULL,
    StatType    VARCHAR(127)    NOT NULL,
    StatData    JSONB           default  NULL,
    Stat        BIGINT          NOT NULL,
    CONSTRAINT PK_Client PRIMARY KEY (Client, Period, StatType, StatData),
    CONSTRAINT FK_Client FOREIGN KEY (Client) REFERENCES clients(id)
);

CREATE INDEX IF NOT EXISTS IDX_Stat_Client
ON client_stats (Client);

INSERT INTO period_meta (Period, Start) VALUES ('Legacy', '2024-10-30');

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
        Period VARCHAR(127),
        StatType VARCHAR(127),
        Data JSONB,
        Stat BIGINT
            ) LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        cs.Period,
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
        ON gd.id = (cs.Data ->> 'gameId')::bigint
    LEFT JOIN game_teams gt
        ON gt.id = gd.id
        AND gt.client = cs.client;
END;
$$;
