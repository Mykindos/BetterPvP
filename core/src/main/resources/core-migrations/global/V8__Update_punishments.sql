START TRANSACTION
--create a temp table
CREATE TABLE IF NOT EXISTS punishments_temp
(
    id              varchar(36)                           not null primary key,
    Client          varchar(36)                           not null,
    Type            varchar(255)                          not null,
    ApplyTime       bigint                                not null
    ExpiryTime      bigint                                not null,
    Reason          varchar(255)                          null,
    Punisher        varchar(36)                           null,
    Revoker         varchar(36)    default null           null,
    RevokeType      varchar(255)   default null           null,
    RevokeTime      bigint         default null           null,
    RevokeReason    varchar(255)   default null           null
);

--convert information
INSERT INTO punishments_temp(id, Client, Type, ApplyTime, ExpiryTime, RevokeTime)
SELECT id, Client, Type, MICROSECOND(TimeApplied) AS ApplyTime, ExpiryTime, MICROSECOND(TimeRevoked) AS RevokeTime FROM punishments;

--alter punishments
ALTER TABLE IF EXISTS punishments
DROP COLUMN TimeApplied,
ADD COLUMN ApplyTime     bigint not null AFTER Type,
DROP COLUMN Revoked,
ADD COLUMN Revoker       varchar(36) default null null AFTER Punisher,
DROP COLUMN TimeRevoked,
ADD COLUMN RevokedType   varchar(255) default null null,
ADD COLUMN RevokedTime   bigint default null null,
ADD COLUMN RevokedReason varchar(255) default null null;

INSERT INTO punishments SELECT * FROM punishments_temp ON DUPLICATE KEY UPDATE;

DROP TABLE punishments_temp;

COMMIT;