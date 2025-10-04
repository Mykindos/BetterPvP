DROP PROCEDURE IF EXISTS GetTopRatingByClass;
CREATE PROCEDURE GetTopRatingByClass(serverVar varchar(64), seasonVar varchar(64), top int, class varchar(20))
BEGIN
    SELECT r.Gamer, r.Rating
    FROM champions_combat_stats as r
    WHERE r.Server = serverVar
      AND r.Season = seasonVar
      AND r.Class = class
      AND r.Valid = 1
    ORDER BY Rating DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKillsByClass;
CREATE PROCEDURE GetTopKillsByClass(serverVar varchar(64), seasonVar varchar(64), top int, class varchar(20))
BEGIN
    SELECT Killer as Gamer, COUNT(*) AS Kills
    FROM kills
             INNER JOIN champions_kills ON kills.Id = champions_kills.KillId
    WHERE Server = serverVar
      AND Season = seasonVar
      AND KillerClass = class
      AND kills.Valid = 1
    GROUP BY Gamer
    ORDER BY Kills DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopDeathsByClass;
CREATE PROCEDURE GetTopDeathsByClass(serverVar varchar(64), seasonVar varchar(64), top int, class varchar(20))
BEGIN
    SELECT Victim as Gamer, COUNT(*) AS Deaths
    FROM kills
             INNER JOIN champions_kills ON kills.Id = champions_kills.KillId
    WHERE Server = serverVar
      AND Season = seasonVar
      AND VictimClass = class
      AND kills.Valid = 1
    GROUP BY Gamer
    ORDER BY Deaths DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKDRByClass;
CREATE PROCEDURE GetTopKDRByClass(serverVar varchar(64), seasonVar varchar(64), top INT, class VARCHAR(20))
BEGIN
    WITH KillCount AS (SELECT Killer AS Gamer, COUNT(*) AS Kills
                       FROM kills
                                LEFT JOIN champions_kills ON kills.Id = champions_kills.KillId
                       WHERE Server = serverVar
                         AND Season = seasonVar
                         AND KillerClass = class
                         AND kills.Valid = 1
                       GROUP BY Gamer),
         Deaths AS (SELECT Victim AS Gamer, COUNT(*) AS Deaths
                    FROM kills
                             LEFT JOIN champions_kills ON kills.Id = champions_kills.KillId
                    WHERE Server = serverVar
                      AND Season = seasonVar
                      AND VictimClass = class
                      AND kills.Valid = 1
                    GROUP BY Gamer)

    SELECT KillCount.Gamer AS Gamer, IFNULL(Kills / Deaths, Kills) AS KDR
    FROM KillCount
             LEFT JOIN Deaths ON KillCount.Gamer = Deaths.Gamer
    ORDER BY KDR DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKillstreakByClass;
CREATE PROCEDURE GetTopKillstreakByClass(serverVar varchar(64), seasonVar varchar(64), top int, class varchar(20))
BEGIN
    SELECT Gamer, Killstreak
    FROM champions_combat_stats AS k
    WHERE k.Server = serverVar
        AND k.Season = seasonVar
        AND k.Class = class
      AND k.Valid = 1
    ORDER BY Killstreak DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopHighestKillstreakByClass;
CREATE PROCEDURE GetTopHighestKillstreakByClass(serverVar varchar(64), seasonVar varchar(64), top INT,
                                                class VARCHAR(20))
BEGIN
    SELECT Gamer, HighestKillstreak AS HighestKillstreak
    FROM champions_combat_stats AS k
    WHERE k.Server = serverVar
      AND k.Season = seasonVar
      AND k.Class = class
      AND k.Valid = 1
    ORDER BY HighestKillstreak DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetChampionsData;
CREATE PROCEDURE GetChampionsData(serverVar varchar(64), seasonVar varchar(64), player varchar(36))
BEGIN
    SELECT cs.Class                      AS Class,
           IFNULL(SUM(kills_count), 0)   AS Kills,
           IFNULL(SUM(deaths_count), 0)  AS Deaths,
           IFNULL(SUM(assists_count), 0) AS Assists,
           MAX(cs.Rating)                AS Rating,
           MAX(cs.Killstreak)            AS Killstreak,
           MAX(cs.HighestKillstreak)     AS HighestKillstreak
    FROM champions_combat_stats AS cs
             LEFT JOIN (SELECT ck.KillerClass, COUNT(KillId) AS kills_count
                        FROM kills
                                 LEFT JOIN champions_kills AS ck ON ck.KillId = Id
                        WHERE Server = serverVar
                          AND Season = seasonVar
                          AND Killer = player
                        GROUP BY ck.KillerClass) AS k ON cs.Class = k.KillerClass
             LEFT JOIN (SELECT VictimClass, COUNT(KillId) AS deaths_count
                        FROM kills
                                 LEFT JOIN champions_kills AS ck ON ck.KillId = Id
                        WHERE Server = serverVar
                          AND Season = seasonVar
                          AND Victim = player
                        GROUP BY VictimClass) AS d ON cs.Class = d.VictimClass
             LEFT JOIN (SELECT ContributorClass, COUNT(KillId) AS assists_count
                        FROM kill_contributions
                                 LEFT JOIN champions_kill_contributions AS ckc ON ckc.ContributionId = Id
                        WHERE Contributor = player
                        GROUP BY ContributorClass) AS ac ON cs.Class = ac.ContributorClass
    WHERE cs.Server = serverVar
      AND cs.Season = seasonVar
      AND cs.Gamer = player
      AND cs.Valid = 1
    GROUP BY cs.Class;
END;