create table if not exists progression_exp
(
    Gamer varchar(36) not null
        primary key,
    Fishing bigint default 0 not null,
    Mining bigint default 0 not null,
    Woodcutting bigint default 0 not null,
    Farming bigint default 0 not null
);

create table if not exists progression_fishing
(
    Gamer     varchar(36)             not null,
    Type      varchar(36)             not null,
    Weight    int                     not null,
    timestamp timestamp default now() not null
);

create table if not exists progression_mining
(
    Gamer varchar(36) not null,
    Material varchar(36) not null,
    AmountMined bigint not null,
    primary key (Gamer, Material)
);

DROP PROCEDURE IF EXISTS GetTopFishingByWeight;
CREATE PROCEDURE GetTopFishingByWeight(IN days DOUBLE, IN maxResults INT)
BEGIN
    SELECT Gamer, SUM(Weight)
    FROM progression_fishing
    WHERE timestamp > NOW() - INTERVAL days DAY
    GROUP BY Gamer
    ORDER BY SUM(Weight) DESC
    LIMIT maxResults;
END;

DROP PROCEDURE IF EXISTS GetTopFishingByCount;
CREATE PROCEDURE GetTopFishingByCount(IN days DOUBLE, IN maxResults INT)
BEGIN
    SELECT Gamer, COUNT(*)
    FROM progression_fishing
    WHERE timestamp > NOW() - INTERVAL days DAY
    GROUP BY Gamer
    ORDER BY COUNT(*) DESC
    LIMIT maxResults;
END;

DROP PROCEDURE IF EXISTS GetGamerFishingWeight;
CREATE PROCEDURE GetGamerFishingWeight(IN gamerName VARCHAR(36), IN days DOUBLE)
BEGIN
    SELECT SUM(Weight) AS TotalWeight
    FROM progression_fishing
    WHERE Gamer = gamerName AND timestamp > NOW() - INTERVAL days DAY;
END;

DROP PROCEDURE IF EXISTS GetGamerFishingCount;
CREATE PROCEDURE GetGamerFishingCount(IN gamerName VARCHAR(36), IN days DOUBLE)
BEGIN
    SELECT COUNT(*) AS FishCount
    FROM progression_fishing
    WHERE Gamer = gamerName AND timestamp > NOW() - INTERVAL days DAY;
END;

DROP PROCEDURE IF EXISTS GetBiggestFishCaught;
CREATE PROCEDURE GetBiggestFishCaught(IN days DOUBLE, IN maxResults INT)
BEGIN
SELECT Gamer, Type, Weight
FROM progression_fishing
WHERE timestamp > NOW() - INTERVAL days DAY
ORDER BY Weight DESC
    LIMIT maxResults;
END;

DROP PROCEDURE IF EXISTS GetBiggestFishCaughtByGamer;
CREATE PROCEDURE GetBiggestFishCaughtByGamer(IN gamer VARCHAR(36), IN days DOUBLE, IN maxResults INT)
BEGIN
SELECT Gamer, Type, Weight
FROM progression_fishing
WHERE timestamp > NOW() - INTERVAL days DAY AND Gamer = gamer
ORDER BY Weight DESC
    LIMIT maxResults;
END;

DROP PROCEDURE IF EXISTS GetTopMiningByOre;
CREATE PROCEDURE GetTopMiningByOre(IN maxResults INT, IN blocks TEXT)
BEGIN
    SET @sql_query = CONCAT('
        SELECT Gamer, SUM(AmountMined)
        FROM progression_mining
        WHERE Material IN (', blocks, ')
        GROUP BY Gamer
        ORDER BY SUM(AmountMined) DESC
        LIMIT ', maxResults, ';'
        );

    PREPARE stmt FROM @sql_query;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END;

DROP PROCEDURE IF EXISTS GetGamerOresMined;
CREATE PROCEDURE GetGamerOresMined(IN gamerName VARCHAR(36), IN blocks TEXT)
BEGIN
    SET @sql_query = CONCAT('
        SELECT SUM(AmountMined)
        FROM progression_mining
        WHERE Gamer = ''', gamerName, ''' AND Material IN (', blocks, ');'
        );

    PREPARE stmt FROM @sql_query;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END

