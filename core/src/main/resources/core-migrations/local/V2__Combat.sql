create table if not exists kills
(
    Id           varchar(36) primary key,
    Killer       varchar(36)    not null,
    Victim       varchar(36)    not null,
    Contribution float          not null,
    Damage       float          not null,
    RatingDelta  int            not null,
    Time         bigint         not null,
    Valid        tinyint        not null default 1
);

create table if not exists kill_contributions
(
    KillId       varchar(36) not null,
    Id           varchar(36) not null,
    Contributor  varchar(36) not null,
    Contribution float       not null,
    Damage       float       not null,
    primary key (Id),
    unique (KillId, Contributor),
    constraint kills_Id_fk
        foreign key (KillId) references kills (Id)
);

create table if not exists combat_stats
(
    Gamer             varchar(36) primary key,
    Rating            int not null,
    Killstreak        int not null default 0,
    HighestKillstreak int not null default 0,
    Valid             tinyint not null default 1
);

DROP PROCEDURE IF EXISTS GetTopRating;
CREATE PROCEDURE GetTopRating(top int)
BEGIN
    SELECT Gamer, Rating FROM combat_stats WHERE Valid = 1 ORDER BY Rating DESC LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKills;
CREATE PROCEDURE GetTopKills(top int)
BEGIN
    SELECT Killer as Gamer, COUNT(*) AS Kills FROM kills WHERE Valid = 1 GROUP BY Gamer ORDER BY Kills DESC LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopDeaths;
CREATE PROCEDURE GetTopDeaths(top int)
BEGIN
    SELECT Victim as Gamer, COUNT(*) AS Deaths FROM kills WHERE Valid = 1 GROUP BY Gamer ORDER BY Deaths DESC LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKDR;
CREATE PROCEDURE GetTopKDR(top int)
BEGIN
    WITH KillCount AS (SELECT Killer AS Gamer, COUNT(*) AS Kills
                       FROM kills
                       WHERE Valid = 1
                       GROUP BY Killer),
         Deaths AS (SELECT Victim AS Gamer, COUNT(*) AS Deaths
                    FROM kills
                    WHERE Valid = 1
                    GROUP BY Victim)

    SELECT KillCount.Gamer AS Gamer, IFNULL(Kills / Deaths, Kills) AS KDR
    FROM KillCount
             LEFT JOIN Deaths ON KillCount.Gamer = Deaths.Gamer
    ORDER BY KDR DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKillstreak;
CREATE PROCEDURE GetTopKillstreak(top int)
BEGIN
    SELECT Gamer, Killstreak FROM combat_stats WHERE Valid = 1 ORDER BY Killstreak DESC LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopHighestKillstreak;
CREATE PROCEDURE GetTopHighestKillstreak(top INT)
BEGIN
    SELECT Gamer, HighestKillstreak FROM combat_stats WHERE Valid = 1 ORDER BY HighestKillstreak DESC LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetCombatData;
CREATE PROCEDURE GetCombatData(player varchar(36))
BEGIN
    SELECT
        IFNULL(SUM(kills_count), 0) AS Kills,
        IFNULL(SUM(deaths_count), 0) AS Deaths,
        IFNULL(SUM(assists_count), 0) AS Assists,
        MAX(cs.Rating) AS Rating,
        MAX(cs.Killstreak) AS Killstreak,
        MAX(cs.HighestKillstreak) AS HighestKillstreak
    FROM combat_stats AS cs
             LEFT JOIN (
        SELECT Killer, COUNT(*) AS kills_count
        FROM kills
        GROUP BY Killer
    ) AS k ON cs.Gamer = k.Killer
             LEFT JOIN (
        SELECT Victim, COUNT(*) AS deaths_count
        FROM kills
        GROUP BY Victim
    ) AS d ON cs.Gamer = d.Victim
             LEFT JOIN (
        SELECT Contributor, COUNT(*) AS assists_count
        FROM kill_contributions
        GROUP BY Contributor
    ) AS ac ON cs.Gamer = ac.Contributor
    WHERE cs.Gamer = player AND cs.Valid = 1;
END;

