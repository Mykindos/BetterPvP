UPDATE kills SET Server = 1, Season = 1;

ALTER TABLE kills MODIFY COLUMN Server tinyint unsigned NOT NULL;
ALTER TABLE kills MODIFY COLUMN Season tinyint unsigned NOT NULL;

UPDATE combat_stats SET Server = 1, Season = 1;

ALTER TABLE combat_stats MODIFY COLUMN Server tinyint unsigned NOT NULL;
ALTER TABLE combat_stats MODIFY COLUMN Season tinyint unsigned NOT NULL;

DROP PROCEDURE IF EXISTS GetCombatData;
CREATE PROCEDURE GetCombatData(player varchar(36), serverVar tinyint unsigned, seasonVar tinyint unsigned)
BEGIN
    SELECT IFNULL(SUM(kills_count), 0)   AS Kills,
           IFNULL(SUM(deaths_count), 0)  AS Deaths,
           IFNULL(SUM(assists_count), 0) AS Assists,
           MAX(cs.Rating)                AS Rating,
           MAX(cs.Killstreak)            AS Killstreak,
           MAX(cs.HighestKillstreak)     AS HighestKillstreak
    FROM combat_stats AS cs
             LEFT JOIN (SELECT Killer, COUNT(*) AS kills_count
                        FROM kills
                        GROUP BY Killer) AS k ON cs.Client = k.Killer
             LEFT JOIN (SELECT Victim, COUNT(*) AS deaths_count
                        FROM kills
                        GROUP BY Victim) AS d ON cs.Client = d.Victim
             LEFT JOIN (SELECT Contributor, COUNT(*) AS assists_count
                        FROM kill_contributions
                        GROUP BY Contributor) AS ac ON cs.Client = ac.Contributor
    WHERE cs.Client = player
      AND Server = serverVar
      AND Season = seasonVar
      AND cs.Valid = 1;
END;
