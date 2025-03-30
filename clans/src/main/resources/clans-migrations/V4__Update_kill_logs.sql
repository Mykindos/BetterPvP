ALTER TABLE `clans_kills`
	ADD COLUMN `KillerName` VARCHAR(16) NOT NULL AFTER `KillId`,
	ADD COLUMN `VictimName` VARCHAR(16) NOT NULL AFTER `KillerClan`;

DROP PROCEDURE IF EXISTS GetClanKillLogs;
CREATE PROCEDURE GetClanKillLogs(IN clanId_param varchar(36))
BEGIN
SELECT KillerName, Killer, KillerClan, VictimName, Victim, VictimClan, Dominance, Time FROM clans_kills
    INNER JOIN kills ON kills.Id = clans_kills.KillId
WHERE KillerClan = clanId_param OR VictimClan = clanId_param
ORDER BY Time DESC;
END;

DROP PROCEDURE IF EXISTS GetClanKillLogsPaged;
CREATE PROCEDURE GetClanKillLogsPaged(IN clanId_param varchar(36), IN offset_param bigint, IN amount_param bigint)
BEGIN
SELECT KillerName, Killer, KillerClan, VictimName, Victim, VictimClan, Dominance, Time FROM clans_kills
    INNER JOIN kills ON kills.Id = clans_kills.KillId
WHERE (KillerClan = clanId_param OR VictimClan = clanId_param)
ORDER BY Time DESC
LIMIT amount_param
OFFSET offset_param;
END;

DROP PROCEDURE IF EXISTS GetClanKillLogsWithClientName;
CREATE PROCEDURE GetClanKillLogsWithClientName(IN clanId_param varchar(36), IN name_param varchar(16), IN offset_param bigint, IN amount_param bigint)
BEGIN
SELECT KillerName, Killer, KillerClan, VictimName, Victim, VictimClan, Dominance, Time FROM clans_kills
    INNER JOIN kills ON kills.Id = clans_kills.KillId
WHERE (KillerClan = clanId_param OR VictimClan = clanId_param)
AND (KillerName = name_param OR VictimName = name_param)
ORDER BY Time DESC
LIMIT amount_param
OFFSET offset_param;
END;

DROP PROCEDURE IF EXISTS GetClanKillLogsWithClanId;
CREATE PROCEDURE GetClanKillLogsWithClanId(IN clanId_param varchar(36), IN filterClanId_param varchar(36), IN offset_param bigint, IN amount_param bigint)
BEGIN
SELECT KillerName, Killer, KillerClan, VictimName, Victim, VictimClan, Dominance, Time FROM clans_kills
    INNER JOIN kills ON kills.Id = clans_kills.KillId
WHERE (KillerClan = clanId_param OR VictimClan = clanId_param)
AND (KillerClan = filterClanId_param OR VictimClan = filterClanId_param)
ORDER BY Time DESC
LIMIT amount_param
OFFSET offset_param;
END;