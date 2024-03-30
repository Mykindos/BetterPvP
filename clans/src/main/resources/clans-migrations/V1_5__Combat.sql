CREATE TABLE IF NOT EXISTS clans_kills
(
    KillId      varchar(36) not null primary key,
    KillerClan  varchar(36) default null,
    VictimClan  varchar(36) default null,
    Dominance   double      default 0
);

DROP PROCEDURE IF EXISTS GetClanKillLogsByClan;
CREATE PROCEDURE GetClanKillLogsByClan(ClanID varchar(36), amount int)
BEGIN
    SELECT UNIX_TIMESTAMP(K.Timestamp) as LogTime, K.Killer, CK.KillerClan, K.Victim, CK.VictimClan, CK.Dominance FROM kills K
    INNER JOIN clans_kills CK ON CK.KillId = K.Id
    WHERE CK.KillerClan = ClanID
    OR CK.VictimClan = ClanID
    LIMIT amount;
END;
