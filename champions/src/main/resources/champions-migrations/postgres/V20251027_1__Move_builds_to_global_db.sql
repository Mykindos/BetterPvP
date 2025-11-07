CREATE TABLE IF NOT EXISTS champions_builds
(
    client    BIGINT REFERENCES clients (id) ON DELETE CASCADE,
    role      VARCHAR(255) NOT NULL,
    id        INTEGER      NOT NULL,
    sword     VARCHAR(255) NULL,
    axe       VARCHAR(255) NULL,
    bow       VARCHAR(255) NULL,
    passive_a VARCHAR(255) NULL,
    passive_b VARCHAR(255) NULL,
    global    VARCHAR(255) NULL,
    active    SMALLINT     NULL,
    PRIMARY KEY (client, role, id)
);