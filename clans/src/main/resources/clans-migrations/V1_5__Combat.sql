CREATE TABLE IF NOT EXISTS clans_kills
(
    KillId      varchar(36) not null primary key,
    KillerClan  varchar(36) default null,
    VictimClan  varchar(36) default null,
    Dominance   double      default 0
);

DROP PROCEDURE IF EXISTS GetClanKillLogsByClan;
CREATE PROCEDURE GetClanKillLogsByClan(ClanID varchar(36))
BEGIN
    SELECT K.Time, CLIENTS1.Name as KillerName, CK.KillerClan, CN1.Name as KillerClanName, CLIENTS2.Name as VictimName, CK.VictimClan, CN2.Name as VictimClanName, CK.Dominance FROM kills K
    INNER JOIN clans_kills CK ON CK.KillId = K.Id
    LEFT JOIN clans_names CN1 ON CN1.id = CK.KillerClan
    LEFT JOIN clans_names CN2 on CN2.id = CK.VictimClan
    LEFT JOIN betterpvp_global.clients as CLIENTS1 ON CLIENTS1.UUID = K.Killer
    LEFT JOIN betterpvp_global.clients as CLIENTS2 ON CLIENTS2.UUID = K.Victim
    WHERE CK.KillerClan = ClanID
    OR CK.VictimClan = ClanID
    ORDER BY K.Time DESC;
END;
