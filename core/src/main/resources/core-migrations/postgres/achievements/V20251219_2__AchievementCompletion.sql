CREATE TABLE IF NOT EXISTS achievement_completions
(
    Id          BIGINT         PRIMARY KEY,
    Client      BIGINT         NOT NULL,
    Namespace   VARCHAR(255)   NOT NULL,
    Keyname     VARCHAR(255)   NOT NULL,
    Timestamp   TIMESTAMP      NOT NULL
);

CREATE INDEX IDX_achievement_completions
ON achievement_completions(Client);

CREATE INDEX IDX_achievement_completions_total
ON achievement_completions(Namespace, Keyname);

CREATE TABLE IF NOT EXISTS achievement_completions_season
(
    Id          BIGINT         PRIMARY KEY REFERENCES achievement_completions(Id),
    Season      SMALLINT       REFERENCES seasons(id) NOT NULL
);

ALTER TABLE achievement_completions_season
ADD CONSTRAINT UC_Season UNIQUE (Id, Season);

CREATE INDEX IDX_achievement_completions_season
ON achievement_completions_season(Id);

CREATE TABLE IF NOT EXISTS achievement_completions_realm
(
    Id          BIGINT         PRIMARY KEY REFERENCES achievement_completions(Id),
    Realm      SMALLINT       REFERENCES realms(id) NOT NULL
);

ALTER TABLE achievement_completions_realm
ADD CONSTRAINT UC_Realm UNIQUE (Id, Realm);

CREATE INDEX IDX_achievement_completions_realm
ON achievement_completions_realm(Id);

CREATE OR REPLACE FUNCTION get_achievement_completions(
    client_param BIGINT
)
    RETURNS TABLE(
        Id BIGINT,
        Client BIGINT,
        Namespace   VARCHAR(255),
        Keyname     VARCHAR(255),
        TimeAchieved   TIMESTAMP,
        Season      SMALLINT,
        Realm       SMALLINT
            ) LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        ac.Id,
        ac.Client,
        ac.Namespace,
        ac.Keyname,
        ac.Timestamp,
        acs.Season,
        acr.Realm
    FROM achievement_completions ac
    LEFT JOIN achievement_completions_season acs
        ON acs.Id = ac.Id
    LEFT JOIN achievement_completions_realm acr
        ON acr.Id = ac.Id
    WHERE ac.Client = client_param;
END;
$$;

CREATE OR REPLACE FUNCTION get_client_achievement_ranks(client_param BIGINT)
RETURNS TABLE
(
    Namespace VARCHAR(255),
    Keyname   VARCHAR(255),
    Rank      BIGINT,
    Season    SMALLINT,
    Realm     SMALLINT
)
LANGUAGE sql
AS $$
    SELECT
        ac.Namespace,
        ac.Keyname,
        COUNT(prev.Id) AS Rank,
        acs.Season,
        acr.Realm
    FROM achievement_completions ac
    LEFT JOIN achievement_completions_season acs
        ON ac.Id = acs.Id
    LEFT JOIN achievement_completions_realm acr
        ON ac.Id = acr.Id

    LEFT JOIN achievement_completions prev
        ON prev.Namespace = ac.Namespace
       AND prev.Keyname   = ac.Keyname
       AND prev.Timestamp < ac.Timestamp

    LEFT JOIN achievement_completions_season prev_s
        ON prev_s.Id = prev.Id

    LEFT JOIN achievement_completions_realm prev_r
        ON prev_r.Id = prev.Id

    WHERE ac.Client = client_param
    GROUP BY
        ac.Namespace,
        ac.Keyname,
        acs.Season,
        acr.Realm;
$$;

CREATE OR REPLACE FUNCTION get_total_achievement_completions()
RETURNS TABLE
(
    Namespace VARCHAR(255),
    Keyname   VARCHAR(255),
    Total      BIGINT,
    Season    SMALLINT,
    Realm     SMALLINT
)
LANGUAGE sql
AS $$
    SELECT
        ac.Namespace,
        ac.Keyname,
        COUNT(ac.Id) AS Total,
        acs.Season,
        acr.Realm
    FROM achievement_completions ac
    LEFT JOIN achievement_completions_season acs
        ON ac.Id = acs.Id
    LEFT JOIN achievement_completions_realm acr
        ON ac.Id = acr.Id
    GROUP BY
        ac.Namespace,
        ac.Keyname,
        acs.Season,
        acr.Realm;
$$;
