create table if not exists progression_exp
(
    Gamer      varchar(36)              not null,
    Profession varchar(255)             not null,
    Experience bigint       default 0   not null,
    primary key (Gamer, Profession)
);


create table if not exists progression_fishing
(
    id        varchar(36)             not null primary key,
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


create table if not exists progression_woodcutting
(
    Gamer varchar(36) not null,
    Material varchar(50) not null,
    Location varchar(285) not null,
    timestamp timestamp default now() not null
);

create table if not exists progression_properties
(
    Gamer      varchar(36)  not null,
    Profession varchar(255) not null,
    Property   varchar(255) not null,
    Value      varchar(255) not null,
    primary key (Gamer, Profession, Property)
);

create table if not exists progression_builds
(
    Gamer      varchar(36)   not null,
    Profession varchar(255)  not null,
    Skill      varchar(255)  not null,
    Level      int default 0 null,
    constraint progression_builds_pk
        primary key (Gamer, Profession, Skill)
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
SELECT id, Gamer, Type, Weight
FROM progression_fishing
WHERE timestamp > NOW() - INTERVAL days DAY
ORDER BY Weight DESC
    LIMIT maxResults;
END;

DROP PROCEDURE IF EXISTS GetBiggestFishCaughtByGamer;
CREATE PROCEDURE GetBiggestFishCaughtByGamer(IN gamer VARCHAR(36), IN days DOUBLE, IN maxResults INT)
BEGIN
SELECT id, Gamer, Type, Weight
FROM progression_fishing
WHERE timestamp > NOW() - INTERVAL days DAY AND progression_fishing.Gamer = gamer
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

