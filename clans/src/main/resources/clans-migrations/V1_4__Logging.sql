
create table if not exists clanlogmeta
(
    logID       varchar(36)     PRIMARY KEY,
    UUID        varchar(36)     not null,
    UUIDType    varchar(12)     not null,
    type        varchar(12)     not null,

    CONSTRAINT clanmemberhistory_id_fk
        FOREIGN KEY (logID) REFERENCES logs (id),
);
