
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

CREATE OR REPLACE FUNCTION IsInClanAtTime(PlayerID varchar(36), ClanID varchar(36, searchTime bigint)) RETURNS BOOL
BEGIN
    CASE WHEN (Count(SELECT DISTINCT L1.id
                        FROM logs L1
                            INNER JOIN clanlogmeta ON logs.id = clanlogmeta.LogUUID
                        WHERE UUID = PlayerID
                        AND UUIDType = 'PLAYER'
                        AND exists (SELECT LogUUID From clanlogmeta C2
                                    WHERE C2.LogUUID = L1.id
                                    AND UUID = ClanID
                                    AND UUIDType = 'CLAN')
                        AND type in ('JOIN', 'CREATE')
                        AND Time < searchTime
                        ORDER BY Time DESC)
            > Count(SELECT DISTINCT L1.id
                      FROM logs L1
                          INNER JOIN clanlogmeta ON logs.id = clanlogmeta.LogUUID
                      WHERE UUID = PlayerID
                      AND UUIDType = 'PLAYER'
                      AND exists (SELECT LogUUID From clanlogmeta C2
                                  WHERE C2.LogUUID = L1.id
                                  AND UUID = ClanID
                                  AND UUIDType = 'CLAN')
                      AND type in ('LEAVE', 'KICKED')
                      AND Time < searchTime
                      ORDER BY Time DESC))
        THEN
            RETURN TRUE;
    ELSE
        RETURN FALSE;
    END;

DROP PROCEDURE IF EXISTS GetWPLogs;
CREATE PROCEDURE GetWPLogs(ClanID varchar(36), amount int)
BEGIN
    SELECT Timestamp, k1.killer, k1.victim FROM kills k1
        WHERE IsInClanAtTime(k1.killer, k1.Timestamp)
        OR IsInClanAtTime(k1.victim, k1.Timestamp)
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
