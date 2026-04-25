CREATE TABLE IF NOT EXISTS client_ips
(
    id         serial PRIMARY KEY,
    client     BIGINT      NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    ip_hash    varchar(64) NOT NULL,
    created_at timestamp   NOT NULL DEFAULT now(),
    last_seen  bigint      NOT NULL DEFAULT extract(epoch from now()),
    CONSTRAINT uk_client_ip UNIQUE (client, ip_hash)
);

