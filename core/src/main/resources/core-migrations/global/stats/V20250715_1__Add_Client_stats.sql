CREATE TABLE IF NOT EXISTS period_meta (
    Period  VARCHAR(127)        PRIMARY KEY,
    Start   DATE     default CURRENT_DATE()
);

CREATE TABLE IF NOT EXISTS client_stats (
    Client      VARCHAR(36)     NOT NULL,
    Period      VARCHAR(127)    NOT NULL,
    Statname    VARCHAR(127)    NOT NULL,
    Stat        DOUBLE          NOT NULL,
    CONSTRAINT PK_Client PRIMARY KEY (Client, Period, Statname),
    CONSTRAINT FK_Client FOREIGN KEY (Client) REFERENCES clients(UUID)
);

CREATE INDEX IDX_Stat_Client
ON client_stats (Client);

INSERT INTO period_meta (Period, Start) VALUES ("Legacy", '2024-10-30');
