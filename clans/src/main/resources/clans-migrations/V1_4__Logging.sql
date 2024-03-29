CREATE VIEW IF NOT EXISTS clanlogs AS
SELECT L1.id as ID, L1.Time, L1.type, C1.Player1, C2.Clan1, C3.Player2, C4.Clan2 FROM logmeta L1
    LEFT JOIN (Select LogUUID, UUID as Player1 FROM logmetauuid WHERE UUIDType = 'PLAYER1') C1 ON C1.LogUUID = L1.id
    LEFT JOIN (Select LogUUID, UUID as Clan1 FROM logmetauuid WHERE UUIDType = 'CLAN1') C2 ON C2.LogUUID = L1.id
    LEFT JOIN (Select LogUUID, UUID as Player2 FROM logmetauuid WHERE UUIDType = 'PLAYER2') C3 ON C3.LogUUID = L1.id
    LEFT JOIN (Select LogUUID, UUID as Clan2 FROM logmetauuid WHERE UUIDType = 'CLAN2') C4 ON C4.LogUUID = L1.id
    WHERE L1.type LIKE 'CLAN_%';

CREATE VIEW IF NOT EXISTS clankills AS
SELECT K.Id, UNIX_TIMESTAMP(K.Timestamp) as Time, K.Killer, CP1.KillerClan, K.Victim, CP2.VictimClan FROM kills K
        LEFT JOIN (SELECT CL.Player1, CL.Clan1 as KillerClan FROM clanlogs CL
                       LEFT JOIN (Select C1.Player1, C1.Clan1, Count(*) as JoinNum from clanlogs C1
                                   WHERE C1.type in ('CLAN_CREATE', 'CLAN_JOIN')
                                   --FILTER TIME HERE
                                   GROUP BY C1.Player1, C1.Clan1)
                                   J1 on J1.Player1 = CL.Player1 AND J1.Clan1 = CL.Clan1
                       LEFt JOIN (Select C2.Player1, C2.Clan1, Count(*) as LeaveNum from clanlogs C2
                                   WHERE C2.type in ('CLAN_LEAVE', 'CLAN_KICK')
                                   --FILTER TIME HERE
                                   GROUP BY C2.Player1, C2.Clan1)
                                   J2 on J2.Player1 = CL.Player1 AND J2.Clan1 = CL.Clan1
                       WHERE (Select Count(*) FROM clanlogs C4 WHERE CL.Clan1 = C4.Clan1
                           --FILTER TIME HERE
                           AND C4.type = 'CLAN_DISBAND') = 0
                       AND J1.JoinNum > IFNULL(J2.LeaveNum, 0)
                       GROUP BY CL.Player1, CL.Clan1
                       ORDER BY CL.Time Desc) as CP1 on CP1.Player1 = K.Killer
        LEFT JOIN (SELECT CL.Player1, CL.Clan1 as VictimClan FROM clanlogs CL
                       LEFT JOIN (Select C1.Player1, C1.Clan1, Count(*) as JoinNum from clanlogs C1
                                   WHERE C1.type in ('CLAN_CREATE', 'CLAN_JOIN')
                                   --FILTER TIME HERE
                                   GROUP BY C1.Player1, C1.Clan1)
                                   J1 on J1.Player1 = CL.Player1 AND J1.Clan1 = CL.Clan1
                       LEFt JOIN (Select C2.Player1, C2.Clan1, Count(*) as LeaveNum from clanlogs C2
                                   WHERE C2.type in ('CLAN_LEAVE', 'CLAN_KICK')
                                   --FILTER TIME HERE
                                   GROUP BY C2.Player1, C2.Clan1)
                                   J2 on J2.Player1 = CL.Player1 AND J2.Clan1 = CL.Clan1
                       WHERE (Select Count(*) FROM clanlogs C4 WHERE CL.Clan1 = C4.Clan1
                           --FILTER TIME HERE
                           AND C4.type = 'CLAN_DISBAND') = 0
                       AND J1.JoinNum > IFNULL(J2.LeaveNum, 0)
                       GROUP BY CL.Player1, CL.Clan1
                       ORDER BY CL.Time Desc) as CP2 on CP2.Player1 = K.Victim;



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

DROP PROCEDURE IF EXISTS GetClanKillLogs;
CREATE PROCEDURE GetClanKillLogs(ClanID varchar(36), amount int)
BEGIN
    SELECT Time, Killer, KillerClan, Victim, VictimClan FROM clankills
    WHERE (KillerClan = ClanID
        OR VictimClan = ClanID)
    LIMIT amount;
END;

--get player, clan
SELECT CL.Player1, CL.Clan1 as KillerClan, J1.JoinNum, J2.LeaveNum FROM clanlogs CL
    LEFT JOIN (Select C1.Player1, C1.Clan1, Count(*) as JoinNum from clanlogs C1
                WHERE C1.type in ('CLAN_CREATE', 'CLAN_JOIN')
                AND C1.Time < Time
                GROUP BY C1.Player1, C1.Clan1)
                J1 on J1.Player1 = CL.Player1 AND J1.Clan1 = CL.Clan1
    LEFt JOIN (Select C2.Player1, C2.Clan1, Count(*) as LeaveNum from clanlogs C2
                WHERE C2.type in ('CLAN_LEAVE', 'CLAN_KICK')
                AND C2.Time < Time
                GROUP BY C2.Player1, C2.Clan1)
                J2 on J2.Player1 = CL.Player1 AND J2.Clan1 = CL.Clan1
    WHERE (Select Count(*) FROM clanlogs C4 WHERE CL.Clan1 = C4.Clan1
        AND C4.Time < Time
        AND C4.type = 'CLAN_DISBAND') = 0
    AND J1.JoinNum > IFNULL(J2.LeaveNum, 0)
    GROUP BY CL.Player1, CL.Clan1
    ORDER BY CL.Time Desc;
