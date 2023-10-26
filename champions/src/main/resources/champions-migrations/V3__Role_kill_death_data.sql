create table if not exists ${tablePrefix}killdeath_data
(
    Matchup varchar(255)  not null,
    Metric  varchar(255)  not null,
    Value   int default 0 null,
    constraint ${tablePrefix}killdeath_data_uk
        unique (Matchup, Metric)
);

create table if not exists ${tablePrefix}kills
(
    KillId      varchar(36) not null primary key,
    KillerClass varchar(20),
    VictimClass varchar(20),
    constraint killId_pk
        foreign key (KillId) references ${coreTablePrefix}kills (Id)
);

create table if not exists ${tablePrefix}kill_contributions
(
    ContributionId   varchar(36) not null primary key,
    ContributorClass varchar(20),
    constraint contributionId_pk
        foreign key (ContributionId) references ${coreTablePrefix}kill_contributions (Id)
);

create table if not exists ${tablePrefix}ratings
(
    Gamer  varchar(36) not null,
    Class  varchar(20),
    Rating int         not null
);

DROP PROCEDURE IF EXISTS GetTopRatingByClass;
CREATE PROCEDURE GetTopRatingByClass(top int, class varchar(20))
BEGIN
    SELECT r.Gamer, r.Rating FROM ${tablePrefix}ratings as r WHERE r.Class = class ORDER BY Rating DESC LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKillsByClass;
CREATE PROCEDURE GetTopKillsByClass(top int, class varchar(20))
BEGIN
    SELECT Killer as Gamer, COUNT(*) AS Kills
    FROM ${coreTablePrefix}kills
             JOIN ${tablePrefix}kills
    WHERE KillerClass = class
    GROUP BY Gamer
    ORDER BY Kills DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopDeathsByClass;
CREATE PROCEDURE GetTopDeathsByClass(top int, class varchar(20))
BEGIN
    SELECT Victim as Gamer, COUNT(*) AS Deaths
    FROM ${coreTablePrefix}kills
             JOIN ${tablePrefix}kills
    WHERE VictimClass = class
    GROUP BY Gamer
    ORDER BY Deaths DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKDRByClass;
CREATE PROCEDURE GetTopKDRByClass(top int, class varchar(20))
BEGIN
    SELECT Kills.Gamer as Gamer, IFNULL(Kills / Deaths, Kills) AS KDR
    FROM (SELECT Killer as Gamer, COUNT(*) AS Kills
          FROM ${coreTablePrefix}kills
                   JOIN ${tablePrefix}kills
          WHERE KillerClass = class
          GROUP BY Gamer) AS Kills
             LEFT JOIN (SELECT Victim as Gamer, COUNT(*) AS Deaths
                        FROM ${coreTablePrefix}
                                 JOIN ${tablePrefix}kills
                        WHERE VictimClass = class
                        GROUP BY Victim) AS Deaths
                       ON Kills.Gamer = Deaths.Gamer
    ORDER BY KDR DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKillstreakByClass;
CREATE PROCEDURE GetTopKillstreakByClass(top int, class varchar(20))
BEGIN
    SELECT Killer as Gamer, COUNT(*) AS Killstreak
    FROM (${coreTablePrefix}kills JOIN ${tablePrefix}kills) as k
    WHERE k.KillerClass = class
      AND k.Timestamp > (SELECT Timestamp
                         FROM ${coreTablePrefix}kills
                                  JOIN ${tablePrefix}kills
                         WHERE Victim = k.Killer
                           AND VictimClass = class
                         ORDER BY Timestamp DESC
                         LIMIT 1)
    GROUP BY Gamer
    ORDER BY Killstreak DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopHighestKillstreakByClass;
CREATE PROCEDURE GetTopHighestKillstreakByClass(top INT, class VARCHAR(20))
BEGIN

    DECLARE done INT DEFAULT FALSE;
    DECLARE cur_killer VARCHAR(36);
    DECLARE cur_victim VARCHAR(36);

    -- Loop through the dataset
    DECLARE cur CURSOR FOR
        SELECT Killer, Victim
        FROM (${coreTablePrefix}kills JOIN ${tablePrefix}kills)
        WHERE KillerClass = class
           OR VictimClass = class
        ORDER BY Timestamp;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    CREATE TEMPORARY TABLE IF NOT EXISTS temp_kill_streaks
    (
        Player            VARCHAR(36) PRIMARY KEY,
        currentStreak     INT,
        highestKillstreak INT DEFAULT 0
    );

    OPEN cur;

    read_loop:
    LOOP
        FETCH cur INTO cur_killer, cur_victim;

        IF done THEN
            LEAVE read_loop;
        END IF;

        -- Update current streaks
        INSERT INTO temp_kill_streaks (Player, currentStreak)
        VALUES (cur_victim, 0)
        ON DUPLICATE KEY UPDATE currentStreak = 0;

        INSERT INTO temp_kill_streaks (Player, currentStreak)
        VALUES (cur_killer, 1)
        ON DUPLICATE KEY UPDATE currentStreak = currentStreak + 1;

        -- Update highest kill streak
        UPDATE temp_kill_streaks
        SET highestKillstreak = GREATEST(highestKillstreak, currentStreak)
        WHERE Player = cur_killer;

    END LOOP;

    CLOSE cur;

    -- Select the top players with the highest kill streaks
    SELECT *
    FROM temp_kill_streaks
    ORDER BY highestKillstreak DESC
    LIMIT top;

    -- Drop the temporary table
    DROP TABLE IF EXISTS temp_kill_streaks;
END;

DROP PROCEDURE IF EXISTS GetChampionsData;
CREATE PROCEDURE GetChampionsData(player varchar(36))
BEGIN
    DECLARE currentKillstreak INT;
    DECLARE highestKillstreak INT;

    -- Call GetHighestKillstreak and capture both columns into variables
    CALL GetHighestKillstreakByClass(player, currentKillstreak, highestKillstreak);

    -- Use the variables in your SELECT statement
    SELECT SUM(IF(Killer = player AND KillerClass = class, 1, 0))                                   as Kills,
           SUM(IF(Victim = player AND KillerClass = class, 1, 0))                                   as Deaths,
           (SELECT COUNT(*)
            FROM (${coreTablePrefix}kill_contributions JOIN ${tablePrefix}kill_contributions)
            WHERE Contributor = player
              AND ContributorClass = class)                                                         as Assists,
           (SELECT Rating FROM ${tablePrefix}ratings as r WHERE Gamer = player AND r.Class = class) as Rating,
           currentKillstreak                                                                        as Killstreak,
           highestKillstreak                                                                        as HighestKillstreak
    FROM (${coreTablePrefix}kills JOIN ${tablePrefix}kills);
END;

DROP PROCEDURE IF EXISTS GetHighestKillstreakByClass;
CREATE PROCEDURE GetHighestKillstreakByClass(player varchar(36), out currentStreak int, out highestKillstreak int)
BEGIN

    DECLARE done INT DEFAULT FALSE;
    DECLARE cur_killer VARCHAR(36);
    DECLARE cur_victim VARCHAR(36);

    -- Loop through the dataset
    DECLARE cur CURSOR FOR
        SELECT Killer, Victim
        FROM (${coreTablePrefix}kills JOIN ${tablePrefix}kills)
        WHERE Killer = player
           OR Victim = player
        ORDER BY Timestamp;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    CREATE TEMPORARY TABLE IF NOT EXISTS temp_kill_streaks
    (
        Player  VARCHAR(36) PRIMARY KEY,
        current INT,
        highest INT DEFAULT 0
    );

    OPEN cur;

    read_loop:
    LOOP
        FETCH cur INTO cur_killer, cur_victim;

        IF done THEN
            LEAVE read_loop;
        END IF;

        -- Update current streaks
        INSERT INTO temp_kill_streaks (Player, current)
        VALUES (cur_victim, 0)
        ON DUPLICATE KEY UPDATE current = 0;

        INSERT INTO temp_kill_streaks (Player, current)
        VALUES (cur_killer, 1)
        ON DUPLICATE KEY UPDATE current = current + 1;

        -- Update highest kill streak
        UPDATE temp_kill_streaks
        SET highest = GREATEST(highest, current)
        WHERE Player = cur_killer;

    END LOOP;

    CLOSE cur;

    -- Select the top players with the highest kill streaks
    SELECT current, highest
    INTO currentStreak, highestKillstreak
    FROM temp_kill_streaks
    LIMIT 1;


    -- Drop the temporary table
    DROP TABLE IF EXISTS temp_kill_streaks;
END;
