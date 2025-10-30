CREATE TABLE IF NOT EXISTS gamer_properties
(
    client   BIGINT       NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    realm    SMALLINT     NOT NULL,
    property VARCHAR(255) NOT NULL,
    value    VARCHAR(255) NULL,
    PRIMARY KEY (client, realm, property)
);
