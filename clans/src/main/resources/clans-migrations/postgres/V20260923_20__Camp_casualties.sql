-- Each player's last death, for the Barracks' Casualty board. Names are stored as component JSON.
CREATE TABLE IF NOT EXISTS camp_casualties
(
    member  VARCHAR(36) PRIMARY KEY,
    server  VARCHAR(64) NOT NULL,
    place   TEXT        NOT NULL,
    zone    TEXT,
    x       INT         NOT NULL,
    y       INT         NOT NULL,
    z       INT         NOT NULL,
    killer  TEXT,
    died_at BIGINT      NOT NULL
);
