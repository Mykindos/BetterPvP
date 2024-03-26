
create table if not exists clanlogmeta
(
    LogUUID     varchar(36)     not null,
    UUID        varchar(36)             ,
    UUIDType    varchar(12)     not null,
    type        varchar(24)     not null,

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

CREATE OR REPLACE FUNCTION NumOfClanLogsOfTypeByClanPlayerAndTime(PlayerID varchar(36), ClanID varchar(36), logType varchar(24), searchTime bigint)
    RETURNS INT
BEGIN
    RETURN (SELECT Count(DISTINCT L1.id)
                    FROM logs L1
                        INNER JOIN clanlogmeta C1 ON L1.id = C1.LogUUID
                    WHERE C1.UUID = PlayerID
                    AND C1.UUIDType = 'PLAYER'
                    AND exists (SELECT LogUUID From clanlogmeta C2
                                WHERE C2.LogUUID = L1.id
                                AND C2.UUID = ClanID
                                AND C2.UUIDType = 'CLAN')
                    AND C1.type = logType
                    AND L1.Time < searchTime);
END;

CREATE OR REPLACE FUNCTION IsInClanAtTime(PlayerID varchar(36), ClanID varchar(36), searchTime bigint) RETURNS BOOL
BEGIN
    CASE WHEN ((NumOfClanLogsOfTypeByClanPlayerAndTime(PlayerID, ClanID, 'JOIN', searchTime)
                + NumOfClanLogsOfTypeByClanPlayerAndTime(PlayerID, ClanID, 'CREATE', searchTime))
            > (NumOfClanLogsOfTypeByClanPlayerAndTime(PlayerID, ClanID, 'LEAVE', searchTime)
                + NumOfClanLogsOfTypeByClanPlayerAndTime(PlayerID, ClanID, 'KICKED', searchTime)
        THEN
            RETURN TRUE;
    ELSE
        RETURN FALSE;
    END;

DROP PROCEDURE IF EXISTS GetWPLogs;
CREATE PROCEDURE GetWPLogs(ClanID varchar(36), amount int)
BEGIN
    SELECT Timestamp, k1.killer, k1.victim FROM kills k1
        WHERE IsInClanAtTime(k1.killer, UNIX_TIMESTAMP(k1.Timestamp)) = TRUE
        OR IsInClanAtTime(k1.victim, UNIX_TIMESTAMP(k1.Timestamp)) = TRUE
        ORDER BY k1.Timestamp DESC
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
