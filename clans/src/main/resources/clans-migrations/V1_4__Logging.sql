
create table if not exists clanlogmeta
(
    LogUUID     varchar(36)     not null,
    UUID        varchar(36)             ,
    UUIDType    varchar(12)     not null,

    CONSTRAINT clanlogmeta_id_fk
        FOREIGN KEY (LogUUID) REFERENCES logtimes (id)
);

create table if not exists clanlogtype
(
    LogUUID varchar(36) not null,
    type    varchar(24) not null
);

CREATE VIEW IF NOT EXISTS clanlogs AS
SELECT L1.id as ID, L1.Time, C1.type, C1.Player1, C2.Clan1, C3.Player2, C4.Clan2 FROM logtimes L1
LEFT JOIN (Select LogUUID, UUID as Player1, type FROM clanlogmeta WHERE UUIDType = 'PLAYER1') C1 ON C1.LogUUID = L1.id
LEFT JOIN (Select LogUUID, UUID as Clan1 FROM clanlogmeta WHERE UUIDType = 'CLAN1') C2 ON C2.LogUUID = L1.id
LEFT JOIN (Select LogUUID, UUID as Player2 FROM clanlogmeta WHERE UUIDType = 'PLAYER2') C3 ON C3.LogUUID = L1.id
LEFT JOIN (Select LogUUID, UUID as Clan2 FROM clanlogmeta WHERE UUIDType = 'CLAN2') C4 ON C4.LogUUID = L1.id;

