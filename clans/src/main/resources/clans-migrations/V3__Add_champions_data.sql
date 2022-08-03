create table if not exists ${tablePrefix}champions_builds
(
    Gamer    varchar(255) not null,
    Role     varchar(255) not null,
    ID       int          not null,
    Sword    varchar(255) null,
    Axe      varchar(255) null,
    Bow      varchar(255) null,
    PassiveA varchar(255) null,
    PassiveB varchar(255) null,
    Global   varchar(255) null,
    Active   tinyint(1)   null
);

alter table  ${tablePrefix}champions_builds
    add constraint  ${tablePrefix}champions_builds_pk
        primary key (Gamer, Role, ID);
