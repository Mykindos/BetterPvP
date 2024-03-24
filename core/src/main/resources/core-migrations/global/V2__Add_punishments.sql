create table if not exists punishments
(
    id          varchar(36)                           not null
        primary key,
    Client      varchar(36)                           not null,
    Type        varchar(255)                          not null,
    ExpiryTime  bigint                                not null,
    Reason      varchar(255)                          null,
    Punisher    varchar(36)                           null,
    TimeApplied timestamp default current_timestamp() not null,
    Revoked     tinyint   default 0                   not null,
    TimeRevoked datetime                              null
);

ALTER TABLE punishments ADD INDEX (Client);
