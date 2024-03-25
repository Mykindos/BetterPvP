
create table if not exists clanlogmeta
(
    LogUUID     varchar(36)     not null,
    UUID        varchar(36)             ,
    UUIDType    varchar(12)     not null,
    type        varchar(12)     not null,

    CONSTRAINT clanlogmeta_id_fk
        FOREIGN KEY (LogUUID) REFERENCES formattedlogs (id)
);

DROP PROCEDURE IF EXISTS GetClanJoinLeaveMessagesByClanUUID;
CREATE PROCEDURE GetClanJoinLeaveMessagesByClanUUID(ClanID varchar(36), amount int)
BEGIN
    SELECT DISTINCT Time, FormattedMessage
    FROM logs
        INNER JOIN clanlogmeta ON logs.id = clanlogmeta.LogUUID
        INNER JOIN formattedlogs ON logs.id = formattedlogs.id
    WHERE UUID = ClanID
    AND UUIDType = 'CLAN'
    AND type in ('JOIN', 'LEAVE', 'KICKER', 'CREATE')
    ORDER BY Time DESC
    LIMIT amount;
END;

DROP PROCEDURE IF EXISTS GetClanLogsByClanUuidAmount;
CREATE PROCEDURE GetClanLogsByClanUuidAmount(UniqueID varchar(36), amount int)
BEGIN
    SELECT DISTINCT Time, FormattedMessage
    FROM logs
        INNER JOIN clanlogmeta ON logs.id = clanlogmeta.LogUUID
        INNER JOIN formattedlogs ON logs.id = formattedlogs.id
    WHERE UUID = UniqueID
    AND UUIDType = 'CLAN'
    ORDER BY Time DESC
    LIMIT amount;
END;
