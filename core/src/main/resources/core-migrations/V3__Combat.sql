create table if not exists ${tablePrefix}kills
(
    Id           varchar(36) primary key,
    Killer       varchar(36)                           not null,
    Victim       varchar(36)                           not null,
    Contribution float                                 not null,
    Damage       float                                 not null,
    RatingDelta  int                                   not null,
    Timestamp    timestamp default current_timestamp() not null
);

create table if not exists ${tablePrefix}kill_contributions
(
    KillId       varchar(36) not null,
    Id           varchar(36) not null,
    Contributor  varchar(36) not null,
    Contribution float       not null,
    Damage       float       not null,
    constraint kills_Id_fk
        foreign key (KillId) references ${tablePrefix}kills (Id)
);

create table if not exists ${tablePrefix}ratings
(
    Gamer  varchar(36) primary key,
    Rating int not null
);

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
    SELECT Kills.Gamer as Gamer, IFNULL(Kills / Deaths, Kills) AS KDR
    FROM (SELECT Killer as Gamer, COUNT(*) AS Kills FROM ${tablePrefix}kills GROUP BY Killer) AS Kills
             LEFT JOIN (SELECT Victim as Gamer, COUNT(*) AS Deaths FROM ${tablePrefix}kills GROUP BY Victim) AS Deaths
                       ON Kills.Gamer = Deaths.Gamer
    ORDER BY KDR DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopKillstreak;
CREATE PROCEDURE GetTopKillstreak(top int)
BEGIN
    SELECT Killer as Gamer, COUNT(*) AS Killstreak
    FROM ${tablePrefix}kills as k
    WHERE k.Timestamp >
          (SELECT Timestamp FROM ${tablePrefix}kills WHERE Victim = k.Killer ORDER BY Timestamp DESC LIMIT 1)
    GROUP BY Gamer
    ORDER BY Killstreak DESC
    LIMIT top;
END;

DROP PROCEDURE IF EXISTS GetTopHighestKillstreak;
CREATE PROCEDURE GetTopHighestKillstreak(top INT)
BEGIN

    DECLARE done INT DEFAULT FALSE;
    DECLARE cur_killer VARCHAR(36);
    DECLARE cur_victim VARCHAR(36);

    -- Loop through the dataset
    DECLARE cur CURSOR FOR
        SELECT Killer, Victim
        FROM ${tablePrefix}kills
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

DROP PROCEDURE IF EXISTS GetCombatData;
CREATE PROCEDURE GetCombatData(player varchar(36))
BEGIN
    DECLARE currentKillstreak INT;
    DECLARE highestKillstreak INT;

    -- Call GetHighestKillstreak and capture both columns into variables
    CALL GetHighestKillstreak(player, currentKillstreak, highestKillstreak);

    -- Use the variables in your SELECT statement
    SELECT SUM(IF(Killer = player, 1, 0))                                                     as Kills,
           SUM(IF(Victim = player, 1, 0))                                                     as Deaths,
           (SELECT COUNT(*) FROM ${tablePrefix}kill_contributions WHERE Contributor = player) as Assists,
           (SELECT Rating FROM ${tablePrefix}ratings WHERE Gamer = player)                    as Rating,
           currentKillstreak                                                                  as Killstreak,
           highestKillstreak                                                                  as HighestKillstreak
    FROM ${tablePrefix}kills;
END;

DROP PROCEDURE IF EXISTS GetHighestKillstreak;
CREATE PROCEDURE GetHighestKillstreak(player varchar(36), out currentStreak int, out highestKillstreak int)
BEGIN

    DECLARE done INT DEFAULT FALSE;
    DECLARE cur_killer VARCHAR(36);
    DECLARE cur_victim VARCHAR(36);

    -- Loop through the dataset
    DECLARE cur CURSOR FOR
        SELECT Killer, Victim
        FROM ${tablePrefix}kills
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