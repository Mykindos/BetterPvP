create table if not exists logs
(
    id        varchar(255)                       primary key,
    Level     varchar(255)                          not null,
    Message   text                                  not null,
    Timestamp timestamp default current_timestamp() not null
);

create table if not exists uuiditems
(
    UUID        varchar(255)    PRIMARY KEY,
    Namespace   varchar(255)    NOT NULL,
    Keyname     varchar(255)    NOT NULL
);

create table if not exists uuidlogmeta
(
    id          varchar(255)    PRIMARY KEY,
    LogUUID     varchar(255)    not null,
    ItemUUID    varchar(255)    not null,
    Type        varchar(255)    not null,
    UUID        varchar(255)    default null,
    UUIDtype    varchar(255)    default null,


    CONSTRAINT uuidlogmeta_id_fk
        FOREIGN KEY (LogUUID) REFERENCES logs (id),
    CONSTRAINT uuidlogmeta_uuid_fk
        FOREIGN KEY (ItemUUID) REFERENCES uuiditems (UUID)
);

DROP PROCEDURE IF EXISTS GetUuidLogsByUuid;
CREATE PROCEDURE GetUuidLogsByUuid(UniqueID varchar(255), amount int)
BEGIN
    SELECT Timestamp, Message FROM logs, uuidlogmeta
        WHERE ItemUUID = UniqueID
        AND LogUUID = logs.id
        ORDER BY Timestamp DESC
        LIMIT amount;
END;

DROP PROCEDURE IF EXISTS GetUuidLogsByPlayer;
CREATE PROCEDURE GetUuidLogsByPlayer(PlayerUuid varchar(255), amount int)
BEGIN
    SELECT Timestamp, Message FROM logs, uuidlogmeta
        WHERE UUID = PlayerUuid
        AND UUIDtype = 'PLAYER'
        AND LogUUID = logs.id
        ORDER BY Timestamp DESC
        LIMIT amount;
END;
