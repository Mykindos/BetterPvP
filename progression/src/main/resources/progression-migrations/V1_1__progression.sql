create table if not exists ${tablePrefix}exp
(
    Gamer varchar(36) not null
        primary key,
    Fishing bigint default 0 not null,
    Mining bigint default 0 not null,
    Woodcutting bigint default 0 not null,
    Farming bigint default 0 not null
);

create table ${tablePrefix}fishing
(
    Gamer     varchar(36)             not null,
    Type      varchar(36)             not null,
    Weight    int                     not null,
    timestamp timestamp default now() not null
);

DROP PROCEDURE IF EXISTS GetTopFishingByWeight;
CREATE PROCEDURE GetTopFishingByWeight(IN days DOUBLE, IN maxResults INT)
BEGIN
    SELECT Gamer, SUM(Weight)
    FROM ${tablePrefix}fishing
    WHERE timestamp > NOW() - INTERVAL days DAY
    GROUP BY Gamer
    ORDER BY SUM(Weight) DESC
    LIMIT maxResults;
END;

DROP PROCEDURE IF EXISTS GetTopFishingByCount;
CREATE PROCEDURE GetTopFishingByCount(IN days DOUBLE, IN maxResults INT)
BEGIN
    SELECT Gamer, COUNT(*)
    FROM ${tablePrefix}fishing
    WHERE timestamp > NOW() - INTERVAL days DAY
    GROUP BY Gamer
    ORDER BY COUNT(*) DESC
    LIMIT maxResults;
END;

DROP PROCEDURE IF EXISTS GetGamerFishingWeight;
CREATE PROCEDURE GetGamerFishingWeight(IN gamerName VARCHAR(36), IN days DOUBLE)
BEGIN
    SELECT SUM(Weight) AS TotalWeight
    FROM ${tablePrefix}fishing
    WHERE Gamer = gamerName AND timestamp > NOW() - INTERVAL days DAY;
END;


DROP PROCEDURE IF EXISTS GetGamerFishingCount;
CREATE PROCEDURE GetGamerFishingCount(IN gamerName VARCHAR(36), IN days DOUBLE)
BEGIN
    SELECT COUNT(*) AS FishCount
    FROM ${tablePrefix}fishing
    WHERE Gamer = gamerName AND timestamp > NOW() - INTERVAL days DAY;
END;
