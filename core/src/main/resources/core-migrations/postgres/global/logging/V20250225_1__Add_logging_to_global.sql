-- Tables
CREATE TABLE IF NOT EXISTS logs
(
    id          BIGINT       PRIMARY KEY,
    realm       SMALLINT     NOT NULL,
    level       VARCHAR(255) NOT NULL,
    action      VARCHAR(255) NULL,
    message     TEXT         NOT NULL,
    log_time    BIGINT       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_logs_level ON logs (level);
CREATE INDEX IF NOT EXISTS idx_logs_server_action ON logs (realm, action);

CREATE TABLE IF NOT EXISTS logs_context
(
    log_id  BIGINT       NOT NULL REFERENCES logs (id) ON DELETE CASCADE,
    realm   SMALLINT     NOT NULL,
    context VARCHAR(255) NOT NULL,
    value   VARCHAR(255) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_logs_context_log_id ON logs_context (log_id);
CREATE INDEX IF NOT EXISTS idx_logs_context_context_value ON logs_context (context, value);

CREATE TABLE IF NOT EXISTS uuiditems
(
    uuid      VARCHAR(36) PRIMARY KEY,
    realm     SMALLINT     NOT NULL,
    namespace VARCHAR(255) NOT NULL,
    keyname   VARCHAR(255) NOT NULL
);

-- Function: GetLogMessagesByContextAndValue
CREATE OR REPLACE FUNCTION get_log_messages_by_context_and_value(
    context_param VARCHAR(255),
    value_param VARCHAR(255),
    realm_param INTEGER
)
    RETURNS TABLE
            (
                message            TEXT,
                action             VARCHAR(255),
                log_time           BIGINT,
                context_values     TEXT
            )
AS
$$
BEGIN
    RETURN QUERY
        SELECT l.message,
               l.action,
               l.log_time,
               STRING_AGG(CONCAT(lc.context, '::', lc.value), '|') as context_values
        FROM logs_context lc
                 INNER JOIN logs l ON lc.log_id = l.id
        WHERE lc.log_id IN (SELECT log_id FROM logs_context WHERE context = context_param AND value = value_param)
          AND l.realm = realm_param
        GROUP BY lc.log_id, l.message, l.action, l.log_time
        ORDER BY l.log_time DESC;
END;
$$ LANGUAGE plpgsql;

-- Function: GetLogMessagesByContextAndAction
CREATE OR REPLACE FUNCTION get_log_messages_by_context_and_action(
    context_param VARCHAR(255),
    value_param VARCHAR(255),
    action_param VARCHAR(255),
    realm_param INTEGER
)
    RETURNS TABLE
            (
                message        TEXT,
                action         VARCHAR(255),
                log_time           BIGINT,
                context_values TEXT
            )
AS
$$
BEGIN
    RETURN QUERY
        SELECT l.message,
               l.action,
               l.log_time,
               STRING_AGG(CONCAT(lc.context, '::', lc.value), '|') as context_values
        FROM logs_context lc
                 INNER JOIN logs l ON lc.log_id = l.id
        WHERE lc.log_id IN (SELECT log_id FROM logs_context WHERE context = context_param AND value = value_param)
          AND l.realm = realm_param
          AND l.action LIKE CONCAT(action_param, '%')
        GROUP BY lc.log_id, l.message, l.action, l.log_time
        ORDER BY l.log_time DESC;
END;
$$ LANGUAGE plpgsql;