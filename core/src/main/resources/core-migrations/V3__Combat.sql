create table if not exists ${tablePrefix}kills
(
    Id varchar(36) primary key,
    Killer varchar(36) not null,
    Victim varchar(36) not null,
    Contribution float not null,
    Damage float not null,
    RatingDelta int not null,
    Timestamp timestamp default current_timestamp() not null
);

create table if not exists ${tablePrefix}kill_contributions
(
    KillId varchar(36) not null,
    Id varchar(36) not null,
    Contributor varchar(36) not null,
    Contribution float not null,
    Damage float not null,
    constraint kills_Id_fk
        foreign key (KillId) references ${tablePrefix}kills (Id)
);

create table if not exists ${tablePrefix}ratings
(
    Gamer varchar(36) primary key,
    Rating int not null
);

DROP PROCEDURE IF EXISTS GetCombatData;
CREATE PROCEDURE GetCombatData(player varchar(36))
BEGIN
    SELECT
        SUM(IF(Killer = player, 1, 0)) AS Kills,
        SUM(IF(Victim = player, 1, 0)) AS Deaths,
        (SELECT COUNT(*) FROM ${tablePrefix}kill_contributions WHERE Contributor = player) AS Assists,
        # Current Kill streak
        # Get the count of kills after the last death (or 0 if no deaths)
        (SELECT COUNT(*) FROM ${tablePrefix}kills WHERE Killer = player AND Timestamp > (SELECT Timestamp FROM ${tablePrefix}kills WHERE Victim = player ORDER BY Timestamp DESC LIMIT 1) OR 0) AS Killstreak,
        # Highest Kill streak
        # Get the highest count of kills after a death (or 0 if no deaths)
        (SELECT MAX(Count) FROM (SELECT COUNT(*) AS Count FROM ${tablePrefix}kills WHERE Killer = player AND Timestamp > (SELECT Timestamp FROM ${tablePrefix}kills WHERE Victim = player ORDER BY Timestamp DESC LIMIT 1) GROUP BY Victim) AS Killstreaks) AS HighestKillstreak,
        # Gets the total rating for the player from the ratings table
        # If it doesn't exist in the ratings table,
        (SELECT Rating FROM ${tablePrefix}ratings WHERE Gamer = player) AS Rating
    FROM ${tablePrefix}kills;
END;

DROP PROCEDURE IF EXISTS GetTopRating;
CREATE PROCEDURE GetTopRating(top int)
BEGIN
    SELECT Gamer, Rating FROM ${tablePrefix}ratings ORDER BY Rating DESC LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKills;
CREATE PROCEDURE GetTopKills(top int)
BEGIN
    SELECT Killer as Gamer, COUNT(*) AS Kills FROM ${tablePrefix}kills GROUP BY Gamer ORDER BY Kills DESC LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopDeaths;
CREATE PROCEDURE GetTopDeaths(top int)
BEGIN
    SELECT Victim as Gamer, COUNT(*) AS Deaths FROM ${tablePrefix}kills GROUP BY Gamer ORDER BY Deaths DESC LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKDR;
CREATE PROCEDURE GetTopKDR(top int)
BEGIN
    SELECT Kills.Gamer as Gamer, Kills / Deaths AS KDR FROM (SELECT Killer as Gamer, COUNT(*) AS Kills FROM ${tablePrefix}kills GROUP BY Killer) AS Kills JOIN (SELECT Victim as Gamer, COUNT(*) AS Deaths FROM ${tablePrefix}kills GROUP BY Victim) AS Deaths ON Kills.Gamer = Deaths.Gamer ORDER BY KDR DESC LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKillstreak;
CREATE PROCEDURE GetTopKillstreak(top int)
BEGIN
    SELECT Killer as Gamer, COUNT(*) AS Killstreak FROM ${tablePrefix}kills WHERE Timestamp > (SELECT Timestamp FROM ${tablePrefix}kills WHERE Victim = Killer ORDER BY Timestamp DESC LIMIT 1) GROUP BY Killer ORDER BY Killstreak DESC LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopHighestKillstreak;
CREATE PROCEDURE GetTopHighestKillstreak(top int)
BEGIN
    SELECT Killer as Gamer, MAX(Count) AS HighestKillstreak FROM (SELECT Killer, COUNT(*) AS Count FROM ${tablePrefix}kills WHERE Timestamp > (SELECT Timestamp FROM ${tablePrefix}kills WHERE Victim = Killer ORDER BY Timestamp DESC LIMIT 1) GROUP BY Killer) AS Killstreaks GROUP BY Killer ORDER BY HighestKillstreak DESC LIMIT top;
END;


