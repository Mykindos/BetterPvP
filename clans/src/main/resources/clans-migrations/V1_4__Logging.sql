
create table if not exists clanmemberhistory
(
    logID       varchar(36)     PRIMARY KEY,
    playerID    varchar(36)     not null,
    clanID      varchar(36)     not null,
    kickerID    varchar(36)     default null,
    type        varchar(10)     not null,

    CONSTRAINT clanmemberhistory_id_fk
        FOREIGN KEY (logID) REFERENCES logs (id),
    CONSTRAINT clanmemberhistory_kickerID_ck
        CHECK (kickerID is null OR type = 'KICK'),
    CONSTRAINT clanmemberhistory_type_ck
            CHECK (KICK IN ('JOIN', 'LEAVE', 'KICK'))
);
