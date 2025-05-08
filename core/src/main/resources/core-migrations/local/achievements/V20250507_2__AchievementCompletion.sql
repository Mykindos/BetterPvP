CREATE TABLE IF NOT EXISTS local_achievement_completions
(
    Id          varchar(36)    PRIMARY KEY,
    User        VARCHAR(36)    NOT NULL,
    Namespace   VARCHAR(255)   NOT NULL,
    Keyname     VARCHAR(255)   NOT NULL,
    Timestamp   TIMESTAMP      NOT NULL
);
