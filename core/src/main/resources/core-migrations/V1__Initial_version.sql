/**
  Demo
 */
create table ${tablePrefix}test
(
    UUID          varchar(255) not null primary key,
    Name          varchar(255) null,
    IP            varchar(255) null,
    `Rank`          varchar(255) null,
    Ignored       blob         null,
    PreviousName  varchar(255) null,
    LastLogin     bigint       null,
    TimePlayed    bigint       null,
    Password      varchar(255) null,
    DiscordLinked tinyint      null,
    AllowVPN      tinyint      null
);
