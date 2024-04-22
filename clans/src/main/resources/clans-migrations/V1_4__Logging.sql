CREATE VIEW IF NOT EXISTS clanlogs AS
SELECT L1.id as ID, L1.Time, L1.type, C1.MainPlayerID, CLIENTS1.Name as MainPlayerName, C2.MainClan, CN1.Name as MainClanName, C3.OtherPlayerID, CLIENTS1.Name as MainPlayerID, C4.OtherClan, CN2.Name as OtherClanName FROM logmeta L1
    LEFT JOIN (Select LogUUID, UUID as MainPlayerID FROM logmetauuid WHERE UUIDType = 'MAINPLAYER') C1 ON C1.LogUUID = L1.id
    LEFT JOIN (Select LogUUID, UUID as MainClan FROM logmetauuid WHERE UUIDType = 'MAINCLAN') C2 ON C2.LogUUID = L1.id
    LEFT JOIN (Select LogUUID, UUID as OtherPlayerID FROM logmetauuid WHERE UUIDType = 'OTHERPLAYER') C3 ON C3.LogUUID = L1.id
    LEFT JOIN (Select LogUUID, UUID as OtherClan FROM logmetauuid WHERE UUIDType = 'OTHERCLAN') C4 ON C4.LogUUID = L1.id
    LEFT JOIN clans_names as CN1 on CN1.id = C2.MainClan
    LEFT JOIN clans_names as CN2 on CN2.id = C4.OtherClan
    LEFT JOIN clients as CLIENTS1 ON CLIENTS1.UUID = C1.MainPlayerID
    LEFT JOIN clients as CLIENTS2 ON CLIENTS2.UUID = C3.OtherPlayerID
    WHERE L1.type LIKE 'CLAN_%';

DROP PROCEDURE IF EXISTS GetClanLogsByClan;
CREATE PROCEDURE GetClanLogsByClan(ClanID varchar(36))
BEGIN
    SELECT DISTINCT CL.Time, CL.type, CL.MainPlayerName, CL.MainClan, CL.MainClanName, CL.OtherPlayerName, CL.OtherClan, CL.OtherClanName
    FROM clanlogs CL
    WHERE CL.MainClan = ClanID
    OR CL.OtherClan = ClanID
    ORDER BY CL.Time DESC;
END;

DROP PROCEDURE IF EXISTS GetClanByPlayerAtTime;
CREATE PROCEDURE GetClanByPlayerAtTime(PlayerID varchar(36), time bigint)
BEGIN
    SELECT CL.MainClan as ClanID, Cl.MainClanName as ClanName FROM clanlogs CL
    WHERE (Select Count(*) FROM clanlogs C1 WHERE CL.MainClan = C1.MainClan
        AND C1.type = 'CLAN_DISBAND'
        AND C1.time < time) = 0
    AND (Select Count(C2.type) FROM clanlogs C2 WHERE CL.MainClan = C2.MainClan
        AND C2.type in ('CLAN_CREATE', 'CLAN_JOIN')
        AND C2.time < time
        AND C2.MainPlayerID = PlayerID) >
        (Select Count(C3.type) FROM clanlogs C3 WHERE CL.MainClan = C3.MainClan
           AND C3.type in ('CLAN_LEAVE', 'CLAN_KICK')
           AND C3.time < time
           AND C3.MainPlayerID = PlayerID)
    GROUP BY CL.MainClan
    ORDER BY CL.Time Desc;
END;

DROP PROCEDURE IF EXISTS GetPlayersByClan;
CREATE PROCEDURE GetPlayersByClan(ClanID varchar(36))
BEGIN
    SELECT DISTINCT CL.MainPlayerName as Player FROM clanlogs CL
    WHERE CL.MainClan = ClanID
    AND CL.type in ('CLAN_JOIN', 'CLAN_CREATE')
    ORDER BY CL.Time DESC;
END;

DROP PROCEDURE IF EXISTS GetClansByPlayer;
CREATE PROCEDURE GetClansByPlayer(PlayerID varchar(36))
BEGIN
    SELECT DISTINCT CL.MainClan as Clan, CL.MainClanName as ClanName FROM clanlogs CL
    WHERE CL.MainPlayerID = PlayerID
    AND CL.type in ('CLAN_JOIN', 'CLAN_CREATE')
    ORDER BY CL.Time DESC;
END;
