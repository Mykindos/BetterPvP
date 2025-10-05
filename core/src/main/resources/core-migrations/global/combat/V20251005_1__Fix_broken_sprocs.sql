DROP PROCEDURE IF EXISTS GetTopRating;
CREATE PROCEDURE GetTopRating(serverVar VARCHAR(255), seasonVar VARCHAR(255), top int)
BEGIN
    SELECT Client, Rating
    FROM combat_stats
    WHERE Server = serverVar
      AND Season = seasonVar
      AND Valid = 1
    ORDER BY Rating DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKills;
CREATE PROCEDURE GetTopKills(serverVar VARCHAR(255), seasonVar VARCHAR(255), top int)
BEGIN
    SELECT Killer as Gamer, COUNT(*) AS Kills
    FROM kills
    WHERE Server = serverVar
      AND Season = seasonVar
      AND Valid = 1
    GROUP BY Gamer
    ORDER BY Kills DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopDeaths;
CREATE PROCEDURE GetTopDeaths(serverVar VARCHAR(255), seasonVar VARCHAR(255), top int)
BEGIN
    SELECT Victim as Gamer, COUNT(*) AS Deaths
    FROM kills
    WHERE Server = serverVar
      AND Season = seasonVar
      AND Valid = 1
    GROUP BY Gamer
    ORDER BY Deaths DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKDR;
CREATE PROCEDURE GetTopKDR(serverVar VARCHAR(255), seasonVar VARCHAR(255), top int)
BEGIN
    WITH KillCount AS (SELECT Killer AS Gamer, COUNT(*) AS Kills
                       FROM kills
                       WHERE Server = serverVar
                         AND Season = seasonvar
                         AND Valid = 1
                       GROUP BY Killer),
         Deaths AS (SELECT Victim AS Gamer, COUNT(*) AS Deaths
                    FROM kills
                    WHERE Server = serverVar
                      AND Season = seasonvar
                      AND Valid = 1
                    GROUP BY Victim)

    SELECT KillCount.Gamer AS Gamer, IFNULL(Kills / Deaths, Kills) AS KDR
    FROM KillCount
             LEFT JOIN Deaths ON KillCount.Gamer = Deaths.Gamer
    ORDER BY KDR DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKillstreak;
CREATE PROCEDURE GetTopKillstreak(serverVar VARCHAR(255), seasonVar VARCHAR(255), top int)
BEGIN
    SELECT Client, Killstreak
    FROM combat_stats
    WHERE Server = serverVar
      AND Season = seasonVar
      AND Valid = 1
    ORDER BY Killstreak DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopHighestKillstreak;
CREATE PROCEDURE GetTopHighestKillstreak(serverVar VARCHAR(255), seasonVar VARCHAR(255), top INT)
BEGIN
    SELECT Client, HighestKillstreak
    FROM combat_stats
    WHERE Server = serverVar
      AND Season = seasonVar
      AND Valid = 1
    ORDER BY HighestKillstreak DESC
    LIMIT top;
END;