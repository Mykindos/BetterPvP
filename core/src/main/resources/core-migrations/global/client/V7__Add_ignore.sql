CREATE TABLE IF NOT EXISTS ignores
(
    Client  VARCHAR(36) NOT NULL,
    Ignored VARCHAR(36) NOT NULL,
    PRIMARY KEY (Client, Ignored)
);

ALTER TABLE ignores ADD INDEX (Client);
