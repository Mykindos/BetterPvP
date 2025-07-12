CREATE TABLE IF NOT EXISTS clans_kills
(
    KillId      varchar(36) not null primary key,
    KillerClan  varchar(36) default null,
    VictimClan  varchar(36) default null,
    Dominance   double      default 0
);

DROP PROCEDURE IF EXISTS GetClanKillLogs;
CREATE PROCEDURE GetClanKillLogs(IN clanId_param varchar(36))
BEGIN
SELECT Killer, KillerClan, Victim, VictimClan, Dominance, Time FROM clans_kills
    INNER JOIN kills ON kills.Id = clans_kills.KillId
WHERE KillerClan = clanId_param OR VictimClan = clanId_param
ORDER BY Time DESC;
END;