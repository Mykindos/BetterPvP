CREATE TABLE IF NOT EXISTS achievement_completions_all
(
    Id          BIGINT         PRIMARY KEY,
    Client      BIGINT         NOT NULL,
    Namespace   VARCHAR(255)   NOT NULL,
    Keyname     VARCHAR(255)   NOT NULL,
    Timestamp   TIMESTAMP      NOT NULL
);

CREATE INDEX IDX_achievement_completions_all
ON achievement_completions_all(Client);

CREATE INDEX IDX_achievement_completions_total_all
ON achievement_completions_all(Namespace, Keyname);

CREATE TABLE IF NOT EXISTS achievement_completions_season
(
    Id          BIGINT    PRIMARY KEY,
    Client      BIGINT         NOT NULL,
    Season      SMALLINT       REFERENCES seasons(id) NOT NULL,
    Namespace   VARCHAR(255)   NOT NULL,
    Keyname     VARCHAR(255)   NOT NULL,
    Timestamp   TIMESTAMP      NOT NULL
);

CREATE INDEX IDX_achievement_completions_season
ON achievement_completions_season(Client);

CREATE INDEX IDX_achievement_completions_total_season
ON achievement_completions_season(Season, Namespace, Keyname);

CREATE TABLE IF NOT EXISTS achievement_completions_realm
(
    Id          BIGINT    PRIMARY KEY,
    Client      BIGINT         NOT NULL,
    Realm       SMALLINT       REFERENCES realms(id) NOT NULL,
    Namespace   VARCHAR(255)   NOT NULL,
    Keyname     VARCHAR(255)   NOT NULL,
    Timestamp   TIMESTAMP      NOT NULL
);

CREATE INDEX IDX_achievement_completions_realm
ON achievement_completions_realm(Client);

CREATE INDEX IDX_achievement_completions_total_realm
ON achievement_completions_realm(Realm, Namespace, Keyname);
