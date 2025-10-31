CREATE TABLE IF NOT EXISTS offline_messages
(
    id        SERIAL PRIMARY KEY,
    client    BIGINT      NOT NULL REFERENCES clients (id),
    time_sent BIGINT      NOT NULL,
    action    VARCHAR(36) NOT NULL,
    message   TEXT        NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_offline_messages_client ON offline_messages (client);
CREATE INDEX IF NOT EXISTS idx_offline_messages_time ON offline_messages (time_sent);


CREATE OR REPLACE FUNCTION get_offline_messages_by_time(
    client_param BIGINT,
    time_param BIGINT
)
    RETURNS TABLE(
        client_uuid VARCHAR(36),
        time_sent BIGINT,
        action VARCHAR(36),
        message TEXT
            ) LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
           clients.uuid,
           offline_messages.time_sent,
           offline_messages.action,
           offline_messages.message

    FROM offline_messages
    INNER JOIN clients ON offline_messages.client = clients.id
    WHERE offline_messages.client = client_param
      AND offline_messages.time_sent > time_param
    ORDER BY offline_messages.time_sent DESC;
END;
$$;