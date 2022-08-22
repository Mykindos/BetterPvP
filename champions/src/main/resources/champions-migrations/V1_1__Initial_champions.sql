create table if not exists ${tablePrefix}builds
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
    Active   tinyint      null
);

alter table  ${tablePrefix}builds
    add constraint  ${tablePrefix}builds_pk
        primary key (Gamer, Role, ID);

# noinspection SqlResolve
INSERT IGNORE INTO property_map VALUES ("ASSASSIN_EQUIPPED", "int");
# noinspection SqlResolve
INSERT IGNORE INTO property_map VALUES ("KNIGHT_EQUIPPED", "int");
# noinspection SqlResolve
INSERT IGNORE INTO property_map VALUES ("RANGER_EQUIPPED", "int");
# noinspection SqlResolve
INSERT IGNORE INTO property_map VALUES ("WARLOCK_EQUIPPED", "int");
# noinspection SqlResolve
INSERT IGNORE INTO property_map VALUES ("PALADIN_EQUIPPED", "int");
# noinspection SqlResolve
INSERT IGNORE INTO property_map VALUES ("GLADIATOR_EQUIPPED", "int");
# noinspection SqlResolve
INSERT IGNORE INTO property_map VALUES ("SKILL_CHAT_PREVIEW", "boolean");