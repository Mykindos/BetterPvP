create table if not exists logs
(
    id        varchar(36)                           primary key,
    Level     varchar(255)                          not null,
    Message   text                                  not null,
    Time      bigint                                not null
);

create table if not exists formattedlogs
(
    id      varchar(36)     PRIMARY KEY,
    Message text            not null

    CONSTRAINT formattedlogs_id_fk
        FOREIGN KEY (id) REFERENCES logs (id)
);

create table if not exists uuiditems
(
    UUID        varchar(36)     PRIMARY KEY,
    Namespace   varchar(255)    NOT NULL,
    Keyname     varchar(255)    NOT NULL
);

create table if not exists uuidlogmeta
(
    id          varchar(36)     PRIMARY KEY,
    LogUUID     varchar(36)     not null,
    ItemUUID    varchar(36)     not null,
    Type        varchar(255)    not null,
    UUID        varchar(36)    default null,
    UUIDtype    varchar(255)    default null,


    CONSTRAINT uuidlogmeta_id_fk
        FOREIGN KEY (LogUUID) REFERENCES logs (id),
    CONSTRAINT uuidlogmeta_uuid_fk
        FOREIGN KEY (ItemUUID) REFERENCES uuiditems (UUID)
);

DROP PROCEDURE IF EXISTS GetUuidLogsByUuid;
CREATE PROCEDURE GetUuidLogsByUuid(UniqueID varchar(36), amount int)
BEGIN
    SELECT DISTINCT Time, Message
    FROM logs
        INNER JOIN uuidlogmeta ON logs.id = uuidlogmeta.LogUUID
    WHERE ItemUUID = UniqueID
    ORDER BY Time DESC
    LIMIT amount;
END;

DROP PROCEDURE IF EXISTS GetUuidLogsByPlayer;
CREATE PROCEDURE GetUuidLogsByPlayer(PlayerUuid varchar(36), amount int)
BEGIN
    SELECT DISTINCT Time, Message
    FROM logs
        INNER JOIN uuidlogmeta ON logs.id = uuidlogmeta.LogUUID
    WHERE UUID = PlayerUuid
      AND UUIDtype = 'PLAYER'
    ORDER BY Time DESC
    LIMIT amount;
END;

ALTER TABLE uuidlogmeta ADD INDEX (UUID);
ALTER TABLE uuidlogmeta ADD INDEX (ItemUUID);
