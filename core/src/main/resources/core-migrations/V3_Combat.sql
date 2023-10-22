create table if not exists ${tablePrefix}kills
(
    Id varchar(36) primary key,
    Killer varchar(36) not null,
    Victim varchar(36) not null,
    RatingDelta int not null,
    Timestamp timestamp default current_timestamp() not null
);

create table if not exists ${tablePrefix}assists
(
    KillId varchar(36) not null,
    Assister varchar(36) not null,
    RatingDelta int not null,
    constraint clans_assists_clans_kills_Id_fk
        foreign key (KillId) references ${tablePrefix}kills (Id)
);

DROP PROCEDURE IF EXISTS GetCombatData;
CREATE PROCEDURE GetCombatData(player varchar(36))
BEGIN
    SELECT
        SUM(IF(Killer = player, 1, 0)) AS Kills,
        SUM(IF(Victim = player, 1, 0)) AS Deaths,
        (SELECT COUNT(*) FROM ${tablePrefix}assists WHERE Assister = player) AS Assists,
        # Current Kill streak
        # Get the count of kills after the last death (or 0 if no deaths)
        (SELECT COUNT(*) FROM ${tablePrefix}kills WHERE Killer = player AND Timestamp > (SELECT Timestamp FROM ${tablePrefix}kills WHERE Victim = player ORDER BY Timestamp DESC LIMIT 1) OR 0) AS Killstreak,
        # Highest Kill streak
        # Get the highest count of kills after a death (or 0 if no deaths)
        (SELECT MAX(Count) FROM (SELECT COUNT(*) AS Count FROM ${tablePrefix}kills WHERE Killer = player AND Timestamp > (SELECT Timestamp FROM ${tablePrefix}kills WHERE Victim = player ORDER BY Timestamp DESC LIMIT 1) GROUP BY Victim) AS Killstreaks) AS HighestKillstreak,
        # Get total rating for the player
        # Adds the rating delta for each kill where the player was the killer
        # Subtracts the rating delta for each kill where the player was the victim
        # Adds the rating delta for each assist where the player was the assister
        ((SELECT SUM(RatingDelta) FROM ${tablePrefix}kills WHERE Killer = player) - (SELECT SUM(RatingDelta) FROM ${tablePrefix}kills WHERE Victim = player) + (SELECT SUM(RatingDelta) FROM ${tablePrefix}assists WHERE Assister = player)) AS Rating
    FROM ${tablePrefix}kills;
END;

DROP PROCEDURE IF EXISTS GetCombatTop;
CREATE PROCEDURE GetCombatTop(top int)
BEGIN
    # Order by rating
    SELECT
        Player,
        SUM(Rating) AS Rating
    FROM
        (
            # Get the rating delta for each kill where the player was the killer
            SELECT
                Killer AS Player,
                SUM(RatingDelta) AS Rating
            FROM ${tablePrefix}kills
            GROUP BY Killer

            UNION ALL

            # Get the rating delta for each assist where the player was the assister
            SELECT
                Assister AS Player,
                SUM(RatingDelta) AS Rating
            FROM ${tablePrefix}assists
            GROUP BY Assister

            UNION ALL

            # Subtracts the rating delta for each kill where the player was the victim
            SELECT
                Victim AS Player,
                -SUM(RatingDelta) AS Rating
            FROM ${tablePrefix}kills
            GROUP BY Victim
        ) AS PlayerRating
    GROUP BY Player
    ORDER BY Rating DESC
    LIMIT top;
END;