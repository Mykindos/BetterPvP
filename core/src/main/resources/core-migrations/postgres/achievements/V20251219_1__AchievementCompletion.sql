CREATE TABLE IF NOT EXISTS achievement_completions
(
    Id          BIGINT    PRIMARY KEY,
    Client      BIGINT         NOT NULL,
    Period      VARCHAR(255)   DEFAULT '' NOT NULL,
    Namespace   VARCHAR(255)   NOT NULL,
    Keyname     VARCHAR(255)   NOT NULL,
    Timestamp   TIMESTAMP      NOT NULL
);

CREATE INDEX IDX_achievement_completions
ON achievement_completions(Client);

CREATE INDEX IDX_achievement_completions_total
ON achievement_completions(Period, Namespace, Keyname);
