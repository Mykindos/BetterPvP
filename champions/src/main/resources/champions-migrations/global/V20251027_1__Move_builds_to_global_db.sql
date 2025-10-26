create table if not exists champions_builds
(
    Gamer    varchar(36)  not null,
    Role     varchar(255) not null,
    ID       int          not null,
    Sword    varchar(255) null,
    Axe      varchar(255) null,
    Bow      varchar(255) null,
    PassiveA varchar(255) null,
    PassiveB varchar(255) null,
    Global   varchar(255) null,
    Active   tinyint      null,
    PRIMARY KEY (Gamer, Role, ID)
);