create table if not exists ${tablePrefix}killdeath_data
(
    Matchup varchar(255)  not null,
    Metric  varchar(255)  not null,
    Value   int default 0 null,
    constraint ${tablePrefix}killdeath_data_uk
        unique (Matchup, Metric)
);

create table if not exists champions_kills
(
    KillId      varchar(36) not null primary key,
    KillerClass varchar(20) default '',
    VictimClass varchar(20) default ''
);

create table if not exists champions_kill_contributions
(
    ContributionId   varchar(36) not null primary key,
    ContributorClass varchar(20) default ''
);

create table if not exists champions_combat_stats
(
    Gamer             varchar(36) not null,
    Class             varchar(20) default '',
    Rating            int         not null,
    Killstreak        int         not null default 0,
    HighestKillstreak int         not null default 0
);

alter table champions_combat_stats
    add constraint champions_combat_stats_pk
        primary key (Gamer, Class);

DROP PROCEDURE IF EXISTS GetTopRatingByClass;
CREATE PROCEDURE GetTopRatingByClass(top int, class varchar(20))
BEGIN
    SELECT r.Gamer, r.Rating
    FROM champions_combat_stats as r
    WHERE r.Class = class
    ORDER BY Rating DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKillsByClass;
CREATE PROCEDURE GetTopKillsByClass(top int, class varchar(20))
BEGIN
    SELECT Killer as Gamer, COUNT(*) AS Kills
    FROM kills
             INNER JOIN champions_kills ON kills.Id = champions_kills.KillId
    WHERE KillerClass = class
    GROUP BY Gamer
    ORDER BY Kills DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopDeathsByClass;
CREATE PROCEDURE GetTopDeathsByClass(top int, class varchar(20))
BEGIN
    SELECT Victim as Gamer, COUNT(*) AS Deaths
    FROM kills
             INNER JOIN champions_kills ON kills.Id = champions_kills.KillId
    WHERE VictimClass = class
    GROUP BY Gamer
    ORDER BY Deaths DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKDRByClass;
CREATE PROCEDURE GetTopKDRByClass(top INT, class VARCHAR(20))
BEGIN
    WITH KillCount AS (
        SELECT Killer AS Gamer, COUNT(*) AS Kills
        FROM kills
                 LEFT JOIN champions_kills ON kills.Id = champions_kills.KillId
        WHERE KillerClass = class
        GROUP BY Gamer
    ),
         Deaths AS (
             SELECT Victim AS Gamer, COUNT(*) AS Deaths
             FROM kills
                      LEFT JOIN champions_kills ON kills.Id = champions_kills.KillId
             WHERE VictimClass = class
             GROUP BY Gamer
         )

    SELECT KillCount.Gamer AS Gamer, IFNULL(Kills / Deaths, Kills) AS KDR
    FROM KillCount
             LEFT JOIN Deaths ON KillCount.Gamer = Deaths.Gamer
    ORDER BY KDR DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKillstreakByClass;
CREATE PROCEDURE GetTopKillstreakByClass(top int, class varchar(20))
BEGIN
    SELECT Gamer, Killstreak
    FROM champions_combat_stats AS k
    WHERE k.Class = class
    ORDER BY Killstreak DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopHighestKillstreakByClass;
CREATE PROCEDURE GetTopHighestKillstreakByClass(top INT, class VARCHAR(20))
BEGIN
    SELECT Gamer, HighestKillstreak AS HighestKillstreak
    FROM champions_combat_stats AS k
    WHERE k.Class = class
    ORDER BY HighestKillstreak DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetChampionsData;
CREATE PROCEDURE GetChampionsData(player varchar(36))
BEGIN
    SELECT
        cs.Class AS Class,
        IFNULL(SUM(kills_count), 0) AS Kills,
        IFNULL(SUM(deaths_count), 0) AS Deaths,
        IFNULL(SUM(assists_count), 0) AS Assists,
        MAX(cs.Rating) AS Rating,
        MAX(cs.Killstreak) AS Killstreak,
        MAX(cs.HighestKillstreak) AS HighestKillstreak
    FROM champions_combat_stats AS cs
             LEFT JOIN (
        SELECT ck.KillerClass, COUNT(KillId) AS kills_count
        FROM kills
                 LEFT JOIN champions_kills AS ck ON ck.KillId = Id
        WHERE Killer = player
        GROUP BY ck.KillerClass
    ) AS k ON cs.Class = k.KillerClass
             LEFT JOIN (
        SELECT VictimClass, COUNT(KillId) AS deaths_count
        FROM kills
                 LEFT JOIN champions_kills AS ck ON ck.KillId = Id
        WHERE Victim = player
        GROUP BY VictimClass
    ) AS d ON cs.Class = d.VictimClass
             LEFT JOIN (
        SELECT ContributorClass, COUNT(KillId) AS assists_count
        FROM kill_contributions
                 LEFT JOIN champions_kill_contributions AS ckc ON ckc.ContributionId = Id
        WHERE Contributor = player
        GROUP BY ContributorClass
    ) AS ac ON cs.Class = ac.ContributorClass
    WHERE cs.Gamer = player
    GROUP BY cs.Class;
END;