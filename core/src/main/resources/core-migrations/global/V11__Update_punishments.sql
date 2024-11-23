-- create a temp table
create table if not exists punishments_temp
(
    id          varchar(36)                           not null
        primary key,
    Client      varchar(36)                           not null,
    Type            varchar(255)                          not null,
    Rule            varchar(255)    default 'CUSTOM'      not null,
    ApplyTime       bigint                                not null,
    ExpiryTime      bigint                                not null,
    Reason          varchar(255)                          null,
    Punisher        varchar(36)                           null,
    Revoker         varchar(36)    default null           null,
    RevokeType      varchar(255)   default null           null,
    RevokeTime      bigint         default null           null,
    RevokeReason    varchar(255)   default null           null
);


-- convert information
INSERT INTO punishments_temp(id, Client, Type, ApplyTime, ExpiryTime, RevokeTime)
SELECT id, Client, Type, UNIX_TIMESTAMP(TimeApplied) AS ApplyTime, ExpiryTime, UNIX_TIMESTAMP(TimeRevoked) AS RevokeTime FROM punishments;

-- alter punishments
ALTER TABLE `punishments`
	ADD COLUMN `Rule` VARCHAR(255) NOT NULL AFTER `Type`,
	ADD COLUMN `ApplyTime` BIGINT NOT NULL AFTER `Rule`,
	ADD COLUMN `Revoker` VARCHAR(36) NULL DEFAULT NULL AFTER `Punisher`,
	ADD COLUMN `RevokeType` VARCHAR(255) NULL DEFAULT NULL AFTER `Revoker`,
	ADD COLUMN `RevokeTime` BIGINT NULL DEFAULT NULL AFTER `RevokeType`,
	ADD COLUMN `RevokeReason` VARCHAR(255) NULL DEFAULT NULL AFTER `RevokeTime`,
	DROP COLUMN `TimeApplied`,
	DROP COLUMN `Revoked`,
	DROP COLUMN `TimeRevoked`;
	
-- update punishments with converted information
REPLACE INTO punishments SELECT * FROM punishments_temp;

-- drop the temp table
DROP TABLE punishments_temp;