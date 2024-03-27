
create table if not exists clanlogmeta
(
    LogUUID     varchar(36)     not null,
    UUID        varchar(36)             ,
    UUIDType    varchar(12)     not null,

    CONSTRAINT logmetauuid_id_fk
        FOREIGN KEY (LogUUID) REFERENCES logtimes (id)
);



CREATE VIEW IF NOT EXISTS clanlogs AS
SELECT L1.id as ID, L1.Time, L1.type, C1.Player1, C2.Clan1, C3.Player2, C4.Clan2 FROM logmeta L1
LEFT JOIN (Select LogUUID, UUID as Player1 FROM logmetauuid WHERE UUIDType = 'PLAYER1') C1 ON C1.LogUUID = L1.id
LEFT JOIN (Select LogUUID, UUID as Clan1 FROM logmetauuid WHERE UUIDType = 'CLAN1') C2 ON C2.LogUUID = L1.id
LEFT JOIN (Select LogUUID, UUID as Player2 FROM logmetauuid WHERE UUIDType = 'PLAYER2') C3 ON C3.LogUUID = L1.id
LEFT JOIN (Select LogUUID, UUID as Clan2 FROM logmetauuid WHERE UUIDType = 'CLAN2') C4 ON C4.LogUUID = L1.id;

DROP PROCEDURE IF EXISTS GetClanLogsByClan;
CREATE PROCEDURE GetClanLogsByClan(ClanID varchar(36), amount int)
BEGIN
    SELECT DISTINCT CL.Time, CL.type, CL.Player1, CL.Clan1, CL.Player2, CL.Clan2
    FROM clanlogs CL
    WHERE CL.Clan1 = ClanID
    OR CL.Clan2 = ClanID
    ORDER BY CL.Time DESC
    LIMIT amount;
END;

