CREATE TABLE IF NOT EXISTS client_stats (
    Client      VARCHAR(36)     NOT NULL,
    Period      VARCHAR(255)    NOT NULL,
    Statname    VARCHAR(127)    NOT NULL,
    Stat        DOUBLE          NOT NULL,
    CONSTRAINT PK_Client PRIMARY KEY (Client, Period, Statname)
);

CREATE INDEX IDX_Stat_Client
ON client_stats (Client);
