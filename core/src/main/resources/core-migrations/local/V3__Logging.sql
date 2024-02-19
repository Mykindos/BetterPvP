create table if not exists logs
(
    id        int auto_increment primary key,
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
    id      int auto_increment  not null,
    logId   int                 not null,
    UUID    varchar(255)        not null,
    Type    varchar(255)        not null,
    Player  varchar(255)        default null,


    CONSTRAINT uuidlogmeta_pk
        PRIMARY KEY (id),
    CONSTRAINT uuidlogmeta_id_fk
        FOREIGN KEY (logId) REFERENCES logs (id),
    CONSTRAINT uuidlogmeta_uuid_fk
        FOREIGN KEY (UUID) REFERENCES uuiditems (UUID),
    CONSTRAINT uuidlogmeta_uq
        UNIQUE (logId, type, player)
);

DROP PROCEDURE IF EXISTS GetUuidLogsByUuid;
CREATE PROCEDURE GetUuidLogsByUuid(UniqueID varchar(255), amount int)
BEGIN
    SELECT Timestamp, Message FROM logs, uuidlogmeta
        WHERE UUID = UniqueID
        AND logId = logs.id
        ORDER BY logs.id DESC
        LIMIT amount;
END;

DROP PROCEDURE IF EXISTS GetUuidLogsByPlayer;
CREATE PROCEDURE GetUuidLogsByPlayer(PlayerUuid varchar(255), amount int)
BEGIN
    SELECT Timestamp, Message FROM logs, uuidlogmeta
        WHERE Player = PlayerUuid
        AND logId = logs.id
        ORDER BY logs.id DESC
        LIMIT amount;
END;
