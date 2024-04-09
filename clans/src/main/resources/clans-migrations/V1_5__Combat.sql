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
    SELECT K.Time, K.Killer, CK.KillerClan, CN1.Name as KillerClanName, K.Victim, CK.VictimClan, CN2.Name as VictimClanName, CK.Dominance FROM kills K
    INNER JOIN clans_kills CK ON CK.KillId = K.Id
    LEFT JOIN clans_names CN1 ON CN1.id = CK.KillerClan
    LEFT JOIN clans_names CN2 on CN2.id = CK.VictimClan
    WHERE CK.KillerClan = ClanID
    OR CK.VictimClan = ClanID
    ORDER BY K.Time DESC;
END;
