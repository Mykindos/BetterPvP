create table if not exists logs
(
    id        varchar(36)                           primary key,
    Level     varchar(255)                          not null,
    Message   text                                  not null,
    Time      bigint                                not null
);

create table if not exists logmeta
(
    id      varchar(36)     PRIMARY KEY,
    Time    bigint          not null,
    Type    varchar(36)
);

create table if not exists logmetauuid
(
    LogUUID     varchar(36)     not null,
    UUID        varchar(36)             ,
    UUIDType    varchar(24)     not null,

    CONSTRAINT logmetauuid_id_fk
        FOREIGN KEY (LogUUID) REFERENCES logmeta (id)
);

create table if not exists loglocations
(
    LogUUID     varchar(36)     not null,
    Name        varchar(36)            ,
    WorldID     varchar(36)     not null,
    X           int             not null,
    Y           int                     ,
    Z           int             not null,

    CONSTRAINT loglocations_id_fk
            FOREIGN KEY (LogUUID) REFERENCES logmeta (id)
);

create table if not exists uuiditems
(
    UUID        varchar(36)     PRIMARY KEY,
    Namespace   varchar(255)    NOT NULL,
    Keyname     varchar(255)    NOT NULL
);

CREATE VIEW IF NOT EXISTS itemlogs AS
SELECT L1.id as ID, L1.Time, L1.Type, LMU1.Item, LMU2.MainPlayerID, LMU3.OtherPlayerID, LL1.Name, LL1.WorldID, LL1.X, LL1.Y, LL1.Z
FROM logmeta L1
LEFT JOIN (Select LogUUID, UUID as Item FROM logmetauuid WHERE UUIDType = 'ITEM') LMU1 ON LMU1.LogUUID = L1.id
LEFT JOIN (Select LogUUID, UUID as MainPlayerID FROM logmetauuid WHERE UUIDType = 'MAINPLAYER') LMU2 ON LMU2.LogUUID = L1.id
LEFT JOIN (Select LogUUID, UUID as OtherPlayerID FROM logmetauuid WHERE UUIDType = 'OTHERPLAYER') LMU3 ON LMU3.LogUUID = L1.id
LEFT JOIN loglocations LL1 ON LL1.LogUUID = L1.id
WHERE L1.Type LIKE 'ITEM_%';


DROP PROCEDURE IF EXISTS GetItemLogsByUuid;
CREATE PROCEDURE GetUuidLogsByUuid(UniqueID varchar(36))
BEGIN
    SELECT DISTINCT IL.Time, IL.Type, IL.Item, CLIENTS1.Name as MainPlayerName, CLIENTS2.Name as OtherPlayerName, IL.Name, IL.WorldID, IL.X, IL.Y, IL.Z
    FROM itemlogs IL
    LEFT JOIN betterpvp_global.clients as CLIENTS1 ON CLIENTS1.UUID = IL.MainPlayerID
    LEFT JOIN betterpvp_global.clients as CLIENTS2 ON CLIENTS2.UUID = IL.OtherPlayerID
    WHERE Item = UniqueID
    ORDER BY Time DESC;
END;

DROP PROCEDURE IF EXISTS GetUuidLogsByPlayer;
CREATE PROCEDURE GetUuidLogsByPlayer(PlayerUuid varchar(36))
BEGIN
    SELECT DISTINCT Time, IL.Type, IL.Item, CLIENTS1.Name as MainPlayerName, CLIENTS2.Name as OtherPlayerName, IL.Name, IL.WorldID, IL.X, IL.Y, IL.Z
        FROM itemlogs IL
        LEFT JOIN betterpvp_global.clients as CLIENTS1 ON CLIENTS1.UUID = IL.MainPlayerID
        LEFT JOIN betterpvp_global.clients as CLIENTS2 ON CLIENTS2.UUID = IL.OtherPlayerID
    WHERE MainPlayerID = PlayerUuid
    OR OtherPlayerID = PlayerUuid
    ORDER BY Time DESC;
END;

ALTER TABLE logmetauuid ADD INDEX (UUID);
