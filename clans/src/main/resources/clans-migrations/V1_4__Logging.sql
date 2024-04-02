CREATE VIEW IF NOT EXISTS clanlogs AS
SELECT L1.id as ID, L1.Time, L1.type, C1.Player1, C2.Clan1, C3.Player2, C4.Clan2 FROM logmeta L1
    LEFT JOIN (Select LogUUID, UUID as Player1 FROM logmetauuid WHERE UUIDType = 'PLAYER1') C1 ON C1.LogUUID = L1.id
    LEFT JOIN (Select LogUUID, UUID as Clan1 FROM logmetauuid WHERE UUIDType = 'CLAN1') C2 ON C2.LogUUID = L1.id
    LEFT JOIN (Select LogUUID, UUID as Player2 FROM logmetauuid WHERE UUIDType = 'PLAYER2') C3 ON C3.LogUUID = L1.id
    LEFT JOIN (Select LogUUID, UUID as Clan2 FROM logmetauuid WHERE UUIDType = 'CLAN2') C4 ON C4.LogUUID = L1.id
    WHERE L1.type LIKE 'CLAN_%';

DROP PROCEDURE IF EXISTS GetClanLogsByClan;
CREATE PROCEDURE GetClanLogsByClan(ClanID varchar(36), amount int)
BEGIN
    SELECT DISTINCT CL.Time, CL.type, CL.Player1, CL.Clan1, CL.Player2, CL.Clan2
    FROM clanlogs CL
    WHERE CL.Clan1 = ClanID
    OR CL.Clan2 = ClanID
    ORDER BY CL.Time DESC
    LIMIT amount;
END;

DROP PROCEDURE IF EXISTS GetClanJoinLeaveLogsByClan;
CREATE PROCEDURE GetClanJoinLeaveLogsByClan(ClanID varchar(36), amount int)
BEGIN
    SELECT DISTINCT CL.Time, CL.type, CL.Player1, CL.Clan1, CL.Player2, CL.Clan2
    FROM clanlogs CL
    WHERE CL.Clan1 = ClanID
    AND CL.type in ('CLAN_CREATE', 'CLAN_JOIN', 'CLAN_LEAVE', 'CLAN_KICK')
    ORDER BY CL.Time DESC
    LIMIT amount;
END;

DROP PROCEDURE IF EXISTS GetClanByPlayerAtTime;
CREATE PROCEDURE GetClanByPlayerAtTime(PlayerID varchar(36), time bigint)
BEGIN
    SELECT CL.Clan1 as ClanID FROM clanlogs CL
    WHERE (Select Count(*) FROM clanlogs C1 WHERE CL.Clan1 = C1.Clan1
        AND C1.type = 'CLAN_DISBAND'
        AND C1.time < time) = 0
    AND (Select Count(C2.type) FROM clanlogs C2 WHERE CL.Clan1 = C2.Clan1
        AND C2.type in ('CLAN_CREATE', 'CLAN_JOIN')
        AND C2.time < time
        AND C2.Player1 = PlayerID) >
        (Select Count(C3.type) FROM clanlogs C3 WHERE CL.Clan1 = C3.Clan1
           AND C3.type in ('CLAN_LEAVE', 'CLAN_KICK')
           AND C3.time < time
           AND C3.Player1 = PlayerID)
    GROUP BY CL.Clan1
    ORDER BY CL.Time Desc;
END;

DROP PROCEDURE IF EXISTS GetPlayersByClan;
CREATE PROCEDURE GetPlayersByClan(ClanID varchar(36))
BEGIN
    SELECT DISTINCT CL.Player1 as Player FROM clanlogs CL
    WHERE CL.Clan1 = ClanID
    AND CL.type in ('CLAN_JOIN', 'CLAN_CREATE')
    ORDER BY CL.Time DESC;
END;

DROP PROCEDURE IF EXISTS GetClansByPlayer;
CREATE PROCEDURE GetClansByPlayer(PlayerID varchar(36))
BEGIN
    SELECT DISTINCT CL.Clan1 as Clan FROM clanlogs CL
    WHERE CL.Player1 = PlayerID
    AND CL.type in ('CLAN_JOIN', 'CLAN_CREATE')
    ORDER BY CL.Time DESC;
END;
