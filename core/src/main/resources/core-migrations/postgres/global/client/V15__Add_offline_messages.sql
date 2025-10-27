INSERT IGNORE INTO property_map VALUES ("LAST_LOGIN", "long");

create table if not exists offline_messages
(
    id        int auto_increment                  primary key,
    Client    varchar(36)                           not null,
    Time      bigint                                not null,
    Action    varchar(36)                           not null,
    Message   text                                  not null
);

ALTER TABLE offline_messages ADD INDEX (Client);
ALTER TABLE offline_messages ADD INDEX (Time);

DROP PROCEDURE IF EXISTS GetOfflineMessagesByTime;
CREATE PROCEDURE GetOfflineMessagesByTime(IN client_param VARCHAR(36), IN time_param bigint)
BEGIN
    SELECT
        offline_messages.Time,
        offline_messages.Action,
        offline_messages.Message
    FROM
        offline_messages
    WHERE
        offline_messages.Client = client_param
    AND
        offline_messages.Time > time_param
    ORDER BY
        offline_messages.Time DESC;
END;
