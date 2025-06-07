CREATE TABLE IF NOT EXISTS achievement_completions
(
    Id          varchar(36)    PRIMARY KEY,
    User        VARCHAR(36)    NOT NULL,
    Period      VARCHAR(255)   DEFAULT '' NOT NULL,
    Namespace   VARCHAR(255)   NOT NULL,
    Keyname     VARCHAR(255)   NOT NULL,
    Timestamp   TIMESTAMP      NOT NULL
);

CREATE INDEX IDX_achievement_completions
ON achievement_completions(User);

CREATE INDEX IDX_achievement_completions_total
ON achievement_completions(Period, Namespace, Keyname);
